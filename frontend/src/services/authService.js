const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export const registerUser = async (userData) => {
  const response = await fetch(`${API_BASE_URL}/api/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message || 'Registration failed. Please make sure the backend is running.');
  }

  return data;
};
