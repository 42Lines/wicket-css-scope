package net.ftlines.css.scoper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CssSyleFragmentContributor {

	Optional<String> getCss();

	static String combine(Collection<CssSyleFragmentContributor> contributors) {
		return contributors.stream().map(CssSyleFragmentContributor::getCss).map(c -> c.orElse(""))
			.collect(Collectors.joining("\n\n"));
	}

	static String combine(CssSyleFragmentContributor... contributors) {
		return combine(Arrays.asList(contributors));
	}

	public static CssSyleFragmentContributor cache(CssSyleFragmentContributor c) {
		if (c instanceof CachingCssSyleFragmentContributor)
			return c;

		return new CachingCssSyleFragmentContributor(c);
	}

	static class CachingCssSyleFragmentContributor implements CssSyleFragmentContributor {

		private CssSyleFragmentContributor delegate;
		Optional<String> cacheValue;

		public CachingCssSyleFragmentContributor(CssSyleFragmentContributor delegate) {
			this.delegate = delegate;
		}

		@Override
		public Optional<String> getCss() {
			if (cacheValue == null) {
				cacheValue = delegate.getCss();
			}
			return cacheValue;
		}

	}

}
