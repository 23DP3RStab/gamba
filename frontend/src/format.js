// One number format for money everywhere on the site, e.g. "1 000,00 €"
const money = new Intl.NumberFormat('lv-LV', { style: 'currency', currency: 'EUR' })

export function formatMoney(value) {
  return money.format(value)
}