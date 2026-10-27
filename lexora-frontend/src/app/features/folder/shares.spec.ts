import { distribute, toShares } from './shares';

const anne = { id: 1, firstName: 'Anne' };
const paul = { id: 2, firstName: 'Paul' };

describe('distribute', () => {
  it('splits equally by default, in 5 centimes', () => {
    expect(distribute(43240, [anne, paul]).map((l) => l.cents)).toEqual([21620, 21620]);
    expect(distribute(43250, [anne, paul]).map((l) => l.cents)).toEqual([21625, 21625]);
  });

  it('follows the weights chosen', () => {
    expect(distribute(10000, [anne, paul], [70, 30]).map((l) => l.cents)).toEqual([7000, 3000]);
    expect(distribute(10000, [anne, paul], [100, 0]).map((l) => l.cents)).toEqual([10000, 0]);
  });

  it('lets a billing customer pay everything', () => {
    expect(distribute(10000, [anne, { ...paul, billing: true }]).map((l) => l.cents)).toEqual([0, 10000]);
  });

  it('gives a single customer the whole amount and nothing below zero', () => {
    expect(distribute(12345, [anne]).map((l) => l.cents)).toEqual([12345]);
    expect(distribute(-500, [anne, paul]).map((l) => l.cents)).toEqual([0, 0]);
  });

  it('converts the lines into API shares', () => {
    expect(toShares(distribute(43240, [anne, paul]))).toEqual([{ customerId: 1, amount: 216.2 }, { customerId: 2, amount: 216.2 }]);
  });
});
