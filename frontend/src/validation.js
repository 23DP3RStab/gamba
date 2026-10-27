// Field rules shared by the register and profile forms.
// Each rule takes the field value and returns an error message, or '' when valid.

const MIN_AGE = 18

// Local date as 'YYYY-MM-DD', the same format a date input gives, so dates
// compare as plain strings without time zone shifts
function isoDate(year, month, day) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${String(year).padStart(4, '0')}-${pad(month)}-${pad(day)}`
}

function nameRule(label) {
  return (v) => {
    if (!v.trim()) return `${label} is required`
    if (v.trim().length > 30) return `${label} can be at most 30 characters`
    return ''
  }
}

export const rules = {
  firstName: nameRule('First name'),
  lastName: nameRule('Last name'),
  birthDate: (v) => {
    if (!v) return 'Date of birth is required'
    const now = new Date()
    const [year, month, day] = [now.getFullYear(), now.getMonth() + 1, now.getDate()]
    if (v > isoDate(year, month, day)) return 'Date of birth cannot be in the future'
    // Born on or before this day MIN_AGE years ago (29 Feb birthdays turn 18 on 1 Mar)
    if (v > isoDate(year - MIN_AGE, month, day)) return `You must be at least ${MIN_AGE} years old`
    return ''
  },
  email: (v) => {
    if (!v.trim()) return 'E-mail is required'
    if (v.trim().length > 40) return 'E-mail can be at most 40 characters'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim())) return 'Enter a valid e-mail address'
    return ''
  },
  phone: (v) => (/^\d{8}$/.test(v) ? '' : 'Phone number must be 8 digits'),
  // The same rule as the backend's RegisterRequest and PasswordRequest
  password: (v) =>
    v.length >= 8 &&
    v.length <= 64 &&
    /[a-z]/.test(v) &&
    /[A-Z]/.test(v) &&
    /\d/.test(v) &&
    /[@#$%^&+=!]/.test(v)
      ? ''
      : '8–64 characters with upper and lower case letters, a digit and one of @#$%^&+=!',
}
