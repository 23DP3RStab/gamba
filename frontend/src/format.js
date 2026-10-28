// One number format for money everywhere on the site, e.g. "1 000,00 €"
const money = new Intl.NumberFormat('lv-LV', { style: 'currency', currency: 'EUR' })

export function formatMoney(value) {
  return money.format(value)
}

// Date and time the same way everywhere, e.g. "28.10.2026 14:05"
const dateTime = new Intl.DateTimeFormat('lv-LV', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatDateTime(value) {
  return dateTime.format(new Date(value))
}