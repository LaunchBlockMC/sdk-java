package gg.launchblock.sdk.util;

public class ImmutablePair<L,R> {
	private final L left;
	private final R right;

	public ImmutablePair(final L left, final R right) {
		this.left = left;
		this.right = right;
	}

	public L getLeft() {
		return left;
	}

	public R getRight() {
		return right;
	}
}
