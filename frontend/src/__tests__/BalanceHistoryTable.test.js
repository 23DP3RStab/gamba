import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BalanceHistoryTable from '../components/wallet/BalanceHistoryTable.vue'

// Newest first, as the server sends them
const ledger = (count) =>
  Array.from({ length: count }, (_, i) => ({
    id: `tx-${i}`,
    amount: i % 2 ? 2 : -1,
    balanceAfter: 100 - i,
    type: i % 2 ? 'WIN' : 'BET',
    description: `Darījums ${i}`,
    createdAt: '2026-10-28T18:00:00Z',
  }))

const rows = (wrapper) => wrapper.findAll('tbody tr')

describe('Bilances vēsture', () => {
  it('H-01 sākumā rāda tikai jaunākās 5 rindas, poga atver visu un aizver atpakaļ', async () => {
    const wrapper = mount(BalanceHistoryTable, { props: { transactions: ledger(8), collapsedRows: 5 } })
    expect(rows(wrapper)).toHaveLength(5)
    expect(rows(wrapper)[0].text()).toContain('Darījums 0')

    const button = wrapper.find('.expand-btn')
    expect(button.text()).toBe('Show all 8 transactions ▾')
    await button.trigger('click')
    expect(rows(wrapper)).toHaveLength(8)
    expect(wrapper.find('.table-wrapper').classes()).toContain('scroll')
    expect(button.text()).toBe('Show less ▴')

    await button.trigger('click')
    expect(rows(wrapper)).toHaveLength(5)
  })

  it('H-02 bez collapsedRows (administratora logs) rāda visu, bez pogas', () => {
    const wrapper = mount(BalanceHistoryTable, { props: { transactions: ledger(8) } })
    expect(rows(wrapper)).toHaveLength(8)
    expect(wrapper.find('.expand-btn').exists()).toBe(false)
  })

  it('H-03 ja rindu ir maz, poga nav vajadzīga', () => {
    const wrapper = mount(BalanceHistoryTable, { props: { transactions: ledger(3), collapsedRows: 5 } })
    expect(rows(wrapper)).toHaveLength(3)
    expect(wrapper.find('.expand-btn').exists()).toBe(false)
  })
})
