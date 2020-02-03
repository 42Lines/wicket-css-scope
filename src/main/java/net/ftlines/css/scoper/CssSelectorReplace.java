package net.ftlines.css.scoper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;

import antlr.css3.css3BaseListener;
import antlr.css3.css3Parser.ClassNameContext;
import antlr.css3.css3Parser.ContainerScopeContext;
import antlr.css3.css3Parser.DeclarationListContext;
import antlr.css3.css3Parser.ExternalContext;
import antlr.css3.css3Parser.HashContext;
import antlr.css3.css3Parser.SelectorContext;
import antlr.css3.css3Parser.SelectorGroupContext;
import antlr.css3.css3Parser.TypeSelectorContext;

public class CssSelectorReplace extends css3BaseListener {

	public static final String SCOPE_PROPERTY = "scope";

	public static final String CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT = "classnamecontext.%s.computed_scope";
	public static final String CLASS_CONTEXT_OPERATION_PROPERTY_FORMAT = "classnamecontext.%s.operation";
	public static final String CLASS_CONTEXT_ORIG_PROPERTY_FORMAT = "classnamecontext.%s.original";

	public static final String HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT = "hashcontext.%s.computed_scope";
	public static final String HASH_CONTEXT_OPERATION_PROPERTY_FORMAT = "hashcontext.%s.operation";
	public static final String HASH_CONTEXT_ORIG_PROPERTY_FORMAT = "hashcontext.%s.original";

	private CssScopeMetadata metadata;

	private TokenStreamRewriter rewriter;
	private List<CssTransformationOperation> operationSequence = new ArrayList<>();
	private int externalAnnotationRefCount = 0;
	private boolean insideDeclaration = false;
	public String scope;

	private boolean debugMode;

	public CssSelectorReplace(BufferedTokenStream tokenStream, CssScopeMetadata metadata, boolean debugMode) {
		this.rewriter = new TokenStreamRewriter(tokenStream);
		this.metadata = metadata;
		this.scope = metadata.getValue(SCOPE_PROPERTY, CssScopeMetadata::generateRandomString);
		this.debugMode = debugMode;
	}

	@Override
	public void enterExternal(ExternalContext ctx) {
		super.enterExternal(ctx);
		externalAnnotationRefCount += 1;
		rewriter.replace(ctx.start, ctx.stop, "");
	}

	@Override
	public void enterContainerScope(ContainerScopeContext ctx) {
		super.enterContainerScope(ctx);

		if (getOperationSequence().stream().noneMatch(this::isOuterScopeAssignement)) {
			CssTransformationOperation op = new CssTransformationOperation();
			op.operation = CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT;
			op.newSelector = "." + scope;
			operationSequence.add(op);
		}
		externalAnnotationRefCount += 1;
		rewriter.replace(ctx.start, ctx.stop, "." + scope + " ");

	}

	@Override
	public void exitSelectorGroup(SelectorGroupContext ctx) {
		super.exitSelectorGroup(ctx);
		if (externalAnnotationRefCount > 0) {
			externalAnnotationRefCount -= 1;
		}
	}

	@Override
	public void enterClassName(ClassNameContext ctx) {
		super.enterClassName(ctx);
		if (externalAnnotationRefCount == 0) {
			CssTransformationOperation op = new CssTransformationOperation();
			op.operation = CssTransformationOperationType.CLASS_REPLACEMENT;
			op.originalSelector = ctx.getText();
			metadata.setValue(getPropertyKey(CLASS_CONTEXT_OPERATION_PROPERTY_FORMAT, op.originalSelector),
				op.operation.name());
			metadata.setValue(getPropertyKey(CLASS_CONTEXT_ORIG_PROPERTY_FORMAT, op.originalSelector),
				op.originalSelector);

			op.newSelector = "."
				+ metadata.getValue(getPropertyKey(CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, op.originalSelector),
					() -> generateNewClassSelector(op.originalSelector));
			operationSequence.add(op);
			rewriter.replace(ctx.start, ctx.stop, op.newSelector);
		}

	}

	@Override
	public void enterHash(HashContext ctx) {
		super.enterHash(ctx);
		if (externalAnnotationRefCount == 0 && !insideDeclaration) {
			CssTransformationOperation op = new CssTransformationOperation();
			op.operation = CssTransformationOperationType.ID_TO_CLASS;
			op.originalSelector = ctx.getText();
			metadata.setValue(getPropertyKey(HASH_CONTEXT_OPERATION_PROPERTY_FORMAT, op.originalSelector),
				op.operation.name());
			metadata.setValue(getPropertyKey(HASH_CONTEXT_ORIG_PROPERTY_FORMAT, op.originalSelector),
				op.originalSelector);
			op.newSelector = "."
				+ metadata.getValue(getPropertyKey(HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, op.originalSelector),
					() -> generateNewClassSelector(op.originalSelector));
			operationSequence.add(op);
			rewriter.replace(ctx.start, ctx.stop, op.newSelector);
		}

	}

	@Override
	public void enterDeclarationList(DeclarationListContext ctx) {
		super.enterDeclarationList(ctx);
		insideDeclaration = true;
	}

	@Override
	public void exitDeclarationList(DeclarationListContext ctx) {
		super.exitDeclarationList(ctx);
		insideDeclaration = false;
	}

	private String generateNewClassSelector(String oldSelector) {
		return scope + "_" + (debugMode ? sanitizeSelector(oldSelector) : CssScopeMetadata.generateRandomString());
	}

	private String sanitizeSelector(String oldSelector) {
		return CssScopeMetadata.generateRandomString() + "_" + oldSelector.replaceAll("[^A-Za-z0-9]", "_");
	}

	@Override
	public void enterSelector(SelectorContext ctx) {
		super.enterSelector(ctx);
		if (externalAnnotationRefCount == 0) {
			if (isUnscopedTypeSelector(ctx)) {
				if (getOperationSequence().stream().noneMatch(this::isOuterScopeAssignement)) {
					CssTransformationOperation op = new CssTransformationOperation();
					op.operation = CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT;
					op.newSelector = "." + scope;
					operationSequence.add(op);
				}

				String origSelector = ctx.getText();

				AtomicBoolean flag = new AtomicBoolean(false);
				String topShared = Arrays.asList(origSelector.strip().split(" ")).stream().map(str -> {
					if (!flag.get()) {
						flag.set(true);
						return str.strip() + "." + scope;
					}
					return str.strip();
				}).collect(Collectors.joining(" "));

				String newSelector = topShared + ", ." + scope + " ";
				rewriter.insertBefore(ctx.start, newSelector);
			}
		}
	}

	private boolean isUnscopedTypeSelector(SelectorContext ctx) {
		if (ctx.getChildCount() > 0) {
			if (ctx.simpleSelectorSequence(0).getChildCount() > 0) {
				return ctx.simpleSelectorSequence(0).getChild(0) instanceof TypeSelectorContext;
			}

		}

		return false;
	}

	public String getOutput() {
		return rewriter.getText();
	}

	private boolean isOuterScopeAssignement(CssTransformationOperation x) {
		return x.operation == CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT;
	}

	public List<CssTransformationOperation> getOperationSequence() {
		return operationSequence;
	}

	public enum CssTransformationOperationType {
		CLASS_REPLACEMENT, ID_TO_CLASS, OUTERSCOPE_ASSIGNMENT
	}

	public static class CssTransformationOperation {
		private CssTransformationOperationType operation;
		private String originalSelector;
		private String newSelector;

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

	public static String getPropertyKey(String format, String selector) {
		return String.format(format, CssScopeMetadata.hashString(selector));
	}

}
