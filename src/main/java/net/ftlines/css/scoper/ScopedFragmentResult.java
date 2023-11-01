package net.ftlines.css.scoper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ScopedFragmentResult {

	private CssScopeMetadata metadata;

	private String oldCss;
	private String oldHtml;

	private String newCss;
	private String newHtml;

	public ScopedFragmentResult(String oldCss, String oldHtml, String newCss, String newHtml,
		CssScopeMetadata metadata) {
		super();
		this.oldCss = oldCss;
		this.oldHtml = oldHtml;
		this.newCss = newCss;
		this.newHtml = newHtml;
		this.metadata = metadata;
	}

	public String getScopedCss() {
		return newCss;
	}

	public String getScopedMarkup() {
		return newHtml;
	}

	public String getOldCss() {
		return oldCss;
	}

	public String getOldHtml() {
		return oldHtml;
	}

	public CssScopeMetadata getMetadata() {
		return metadata;
	}

	public static ScopedFragmentResult transform(String css, String markup, CssScopeMetadata metadata,
		boolean debugMode) {

		CssSelectorReplace replace = CssSelectorReplace.parse(css, metadata, debugMode);

		Document doc = Jsoup.parseBodyFragment(markup);
		doc.outputSettings(new Document.OutputSettings().prettyPrint(false));

		for (CssTransformationOperation rule : replace.getOperationSequence()) {
			if (rule.getOperation() == CssTransformationOperationType.CLASS_REPLACEMENT) {
				doc.getElementsByClass(rule.getOriginalSelector().substring(1))
					.addClass(rule.getNewSelector().substring(1)).removeClass(rule.getOriginalSelector().substring(1));
			}

			if (rule.getOperation() == CssTransformationOperationType.ID_TO_CLASS) {
				Element elem = doc.getElementById(rule.getOriginalSelector().substring(1));
				if (elem != null) {
					elem.addClass(rule.getNewSelector().substring(1)).removeAttr("id");
				}
			}

			if (rule.getOperation() == CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT) {
				doc.body().children().addClass(rule.getNewSelector().substring(1));
			}
		}

		return new ScopedFragmentResult(css, markup, replace.getOutput(), doc.body().html().toString(), metadata);
	}

	@Override
	public String toString() {
		return "ScopedFragmentResult [newCss=" + newCss + ", newHtml=" + newHtml + "]";
	}
	
	public enum CssTransformationOperationType {
		CLASS_REPLACEMENT, ID_TO_CLASS, OUTERSCOPE_ASSIGNMENT
	}

	public static class CssTransformationOperation {
		private CssTransformationOperationType operation;
		private String originalSelector;
		private String newSelector;

		public CssTransformationOperation(CssTransformationOperationType operation, String originalSelector,
			String newSelector) {
			super();
			this.operation = operation;
			this.originalSelector = originalSelector;
			this.newSelector = newSelector;
		}

		public CssTransformationOperationType getOperation() {
			return operation;
		}

		public String getOriginalSelector() {
			return originalSelector;
		}

		public String getNewSelector() {
			return newSelector;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((newSelector == null) ? 0 : newSelector.hashCode());
			result = prime * result + ((operation == null) ? 0 : operation.hashCode());
			result = prime * result + ((originalSelector == null) ? 0 : originalSelector.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CssTransformationOperation other = (CssTransformationOperation) obj;
			if (newSelector == null) {
				if (other.newSelector != null)
					return false;
			} else if (!newSelector.equals(other.newSelector))
				return false;
			if (operation != other.operation)
				return false;
			if (originalSelector == null) {
				if (other.originalSelector != null)
					return false;
			} else if (!originalSelector.equals(other.originalSelector))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "CssTransformationOperation [operation=" + operation + ", originalSelector=" + originalSelector
				+ ", newSelector=" + newSelector + "]";
		}

	}


}
