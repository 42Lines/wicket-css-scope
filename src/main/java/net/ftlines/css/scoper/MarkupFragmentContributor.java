package net.ftlines.css.scoper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public interface MarkupFragmentContributor {

	Optional<String> getMarkup();

	public static String combine(Collection<MarkupFragmentContributor> contributors) {
		return contributors.stream().map(MarkupFragmentContributor::getMarkup).map(c -> c.orElse(""))
			.collect(Collectors.joining("\n\n"));
	}

	public static String combine(MarkupFragmentContributor... contributors) {
		return combine(Arrays.asList(contributors));
	}

	public static MarkupFragmentContributor cache(MarkupFragmentContributor c) {
		if (c instanceof CachingMarkupFragmentContributor)
			return c;

		return new CachingMarkupFragmentContributor(c);
	}

	static class CachingMarkupFragmentContributor implements MarkupFragmentContributor {

		private MarkupFragmentContributor delegate;
		Optional<String> cacheValue;

		public CachingMarkupFragmentContributor(MarkupFragmentContributor delegate) {
			this.delegate = delegate;
		}

		@Override
		public Optional<String> getMarkup() {
			if (cacheValue == null) {
				cacheValue = delegate.getMarkup();
			}
			return cacheValue;
		}

	}

}
