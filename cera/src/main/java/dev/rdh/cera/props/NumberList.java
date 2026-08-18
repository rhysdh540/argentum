package dev.rdh.cera.props;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;

public final class NumberList {
	private final long[] ranges;

	private NumberList(long[] ranges) {
		this.ranges = ranges;
	}

	public boolean contains(int value) {
		int lo = 0, hi = ranges.length;

		while (lo < hi) {
			int mid = (lo + hi) >>> 1;

			if ((int) (ranges[mid] >> 32) <= value)
				lo = mid + 1;
			else
				hi = mid;
		}

		return lo != 0 && value <= (int) ranges[lo - 1];
	}

	public static Result<NumberList> parse(String input) {
		if (input == null)
			return Result.failure("Input is null");

		return new Parser(input).parse();
	}

	private static long pack(int start, int end) {
		return ((long) start << 32) | (end & 0xffffffffL);
	}

	private static Result<NumberList> bad(int index) {
		return Result.failure("Invalid number list at index " + index);
	}

	private static long[] normalize(long[] ranges) {
		Arrays.sort(ranges);

		int out = 0;
		for (long range : ranges) {
			int start = (int) (range >> 32);
			int end = (int) range;

			if (out != 0) {
				long prev = ranges[out - 1];
				int prevEnd = (int) prev;

				if ((long) start <= (long) prevEnd + 1) {
					if (end > prevEnd)
						ranges[out - 1] = pack((int) (prev >> 32), end);
					continue;
				}
			}

			ranges[out++] = range;
		}

		return Arrays.copyOf(ranges, out);
	}

	private static final class Parser {
		private final String s;
		private final int n;
		private int i;

		Parser(String s) {
			this.s = s;
			this.n = s.length();
		}

		Result<NumberList> parse() {
			LongArrayList list = new LongArrayList();

			while (true) {
				while (i < n && s.charAt(i) == ' ') i++;
				if (i == n) break;

				int start = i, a, b;

				if (s.charAt(i) == '-') {
					a = b = number(i + 1, true);
					if (i < 0) return bad(start);
				} else {
					a = wrappedNumber();
					if (i < 0) return bad(start);

					if (i < n && s.charAt(i) == '-') {
						int dash = i++;

						// open-ended range, e.g. "100-"
						if (i == n || s.charAt(i) == ' ') {
							b = Integer.MAX_VALUE;
						} else {
							b = wrappedNumber();
							if (i < 0) return bad(dash + 1);
						}
					} else {
						b = a;
					}
				}

				if (i < n && s.charAt(i) != ' ')
					return bad(i);

				if (a > b)
					return bad(start);

				list.add(pack(a, b));
			}

			return Result.success(new NumberList(normalize(list.toLongArray())));
		}

		private int wrappedNumber() {
			if (i >= n) {
				i = -1;
				return 0;
			}

			if (s.charAt(i) != '(')
				return number(i, false);

			int j = i + 1;
			if (j >= n) {
				i = -1;
				return 0;
			}

			boolean negative = s.charAt(j) == '-';
			if (negative) j++;

			int result = number(j, negative);
			if (i < 0) return 0;

			if (i >= n || s.charAt(i) != ')') {
				i = -1;
				return 0;
			}
			i++;

			return result;
		}

		private int number(int j, boolean negative) {
			int start = j;

			while (j < n) {
				int d = s.charAt(j) - '0';
				if (d < 0 || d > 9) break;
				j++;
			}

			if (j == start) {
				i = -1;
				return 0;
			}

			try {
				int result = Integer.parseInt(s, negative ? start - 1 : start, j, 10);
				i = j;
				return result;
			} catch (NumberFormatException e) {
				i = -1;
				return 0;
			}
		}
	}
}
