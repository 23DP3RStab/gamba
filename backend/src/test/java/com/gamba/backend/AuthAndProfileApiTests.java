package com.gamba.backend;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gamba.backend.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndProfileApiTests {

	private static final String ANNA = """
			{"firstName": "Anna", "lastName": "Berzina", "birthDate": "2000-05-17",
			 "email": "Anna@Example.com", "phone": "21234567", "password": "!Janka68"}
			""";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserRepository users;

	@AfterEach
	void deleteUsers() {
		users.deleteAll();
	}

	@Test
	void registerReturnsTokenAndStoresEmailInLowerCase() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(ANNA))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.user.email").value("anna@example.com"))
			.andExpect(jsonPath("$.user.birthDate").value("2000-05-17"));
	}

	@Test
	void registerRejectsAnEmailThatIsTaken() throws Exception {
		register(ANNA);
		String sameEmail = ANNA.replace("Anna@Example.com", "ANNA@example.com");
		mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(sameEmail))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.email").value("This e-mail is already registered"));
	}

	@Test
	void registerReportsEachInvalidField() throws Exception {
		String invalid = """
				{"firstName": "", "lastName": "Berzina", "birthDate": "2999-01-01",
				 "email": "abc.lv", "phone": "1234567", "password": "1234567"}
				""";
		mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").value("First name is required"))
			.andExpect(jsonPath("$.errors.birthDate").value("Date of birth cannot be in the future"))
			.andExpect(jsonPath("$.errors.email").value("Enter a valid e-mail address"))
			.andExpect(jsonPath("$.errors.phone").value("Phone number must be 8 digits"))
			.andExpect(jsonPath("$.errors.password").value(startsWith("At least 8 characters")))
			.andExpect(jsonPath("$.errors.lastName").doesNotExist());
	}

	@Test
	void loginNeedsTheRightPassword() throws Exception {
		register(ANNA);
		login("anna@example.com", "!Janka68").andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty());
		login("anna@example.com", "!Janka69").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("Wrong e-mail or password"));
	}

	@Test
	void profileNeedsAValidToken() throws Exception {
		mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/profile").header(AUTHORIZATION, bearer("not-a-token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void profileCanBeReadAndUpdated() throws Exception {
		String token = register(ANNA);
		mvc.perform(get("/api/profile").header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Anna"));

		String changed = """
				{"firstName": "Anete", "lastName": "Kalnina", "birthDate": "1999-12-31",
				 "email": "anete@example.com", "phone": "29876543"}
				""";
		mvc.perform(put("/api/profile").header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON).content(changed))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("anete@example.com"));

		mvc.perform(get("/api/profile").header(AUTHORIZATION, bearer(token)))
			.andExpect(jsonPath("$.firstName").value("Anete"))
			.andExpect(jsonPath("$.lastName").value("Kalnina"))
			.andExpect(jsonPath("$.birthDate").value("1999-12-31"))
			.andExpect(jsonPath("$.phone").value("29876543"));
	}

	@Test
	void profileCannotTakeAnotherUsersEmail() throws Exception {
		register(ANNA.replace("Anna@Example.com", "other@example.com"));
		String token = register(ANNA);
		String update = """
				{"firstName": "Anna", "lastName": "Berzina", "birthDate": "2000-05-17",
				 "email": "Other@example.com", "phone": "21234567"}
				""";
		mvc.perform(put("/api/profile").header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON).content(update))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.errors.email").value("This e-mail is already registered"));
	}

	@Test
	void passwordChangeChecksTheCurrentPassword() throws Exception {
		String token = register(ANNA);
		changePassword(token, "!Wrong123", "N3w!password").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.current").value("Current password is wrong"));
		changePassword(token, "!Janka68", "weak").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.next").value(startsWith("At least 8 characters")));
		changePassword(token, "!Janka68", "N3w!password").andExpect(status().isNoContent());

		login("anna@example.com", "!Janka68").andExpect(status().isUnauthorized());
		login("anna@example.com", "N3w!password").andExpect(status().isOk());
	}

	@Test
	void deletedAccountIsGone() throws Exception {
		String token = register(ANNA);
		mvc.perform(delete("/api/profile").header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isNoContent());

		mvc.perform(get("/api/profile").header(AUTHORIZATION, bearer(token)))
			.andExpect(status().isUnauthorized());
		login("anna@example.com", "!Janka68").andExpect(status().isUnauthorized());
	}

	private String register(String body) throws Exception {
		String response = mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(response, "$.token");
	}

	private ResultActions login(String email, String password) throws Exception {
		String body = """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password);
		return mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(body));
	}

	private ResultActions changePassword(String token, String current, String next) throws Exception {
		String body = """
				{"current": "%s", "next": "%s"}
				""".formatted(current, next);
		return mvc.perform(put("/api/profile/password").header(AUTHORIZATION, bearer(token))
				.contentType(APPLICATION_JSON).content(body));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
