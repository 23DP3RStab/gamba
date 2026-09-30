// Field rules shared by the register and profile forms.
// Each rule takes the field value and returns an error message, or '' when valid.

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
    if (new Date(v) > new Date()) return 'Date of birth cannot be in the future'
    return ''
  },
  email: (v) => {
    if (!v.trim()) return 'E-mail is required'
    if (v.trim().length > 40) return 'E-mail can be at most 40 characters'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim())) return 'Enter a valid e-mail address'
    return ''
  },
  phone: (v) => (/^\d{8}$/.test(v) ? '' : 'Phone number must be 8 digits'),
  password: (v) =>
    v.length >= 8 && /[a-z]/.test(v) && /[A-Z]/.test(v) && /\d/.test(v) && /[^A-Za-z0-9]/.test(v)
      ? ''
      : 'At least 8 characters with upper and lower case letters, a digit and a symbol',
}
