package dev.rdh.cera.props;

import java.util.Objects;

public final class Result<T> {
	private final T value;
	private final String error;

	private Result(T value, String error) {
		this.value = value;
		this.error = error;
	}

	public static <T> Result<T> success(T value) {
		return new Result<>(value, null);
	}

	public static <T> Result<T> failure(String error) {
		return new Result<>(null, Objects.requireNonNull(error));
	}

	public boolean isSuccess() {
		return this.error == null;
	}

	public String error() {
		if (this.isSuccess()) {
			throw new IllegalStateException("Cannot get error from a successful result");
		}
		return this.error;
	}

	public T value() {
		if (!this.isSuccess()) {
			throw new IllegalStateException("Cannot get value from a failed result");
		}
		return this.value;
	}

	public T orElse(T fallback) {
		return this.isSuccess() ? this.value : fallback;
	}
}
