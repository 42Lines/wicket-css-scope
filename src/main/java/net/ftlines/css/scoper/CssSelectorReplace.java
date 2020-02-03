package net.ftlines.css.scoper;

import static org.antlr.v4.runtime.CharStreams.fromString;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import antlr.css3.css3BaseListener;
import antlr.css3.css3Lexer;
import antlr.css3.css3Parser;
import antlr.css3.css3Parser.ClassNameContext;
import antlr.css3.css3Parser.ContainerScopeContext;
import antlr.css3.css3Parser.DeclarationListContext;
import antlr.css3.css3Parser.ExternalContext;
import antlr.css3.css3Parser.HashContext;
import antlr.css3.css3Parser.KnownRulesetContext;
import antlr.css3.css3Parser.SelectorContext;
import antlr.css3.css3Parser.SelectorGroupContext;
import antlr.css3.css3Parser.TypeSelectorContext;
import antlr.css3.css3Parser.UnknownRulesetContext;
import net.ftlines.css.scoper.ScopedFragmentResult.CssTransformationOperation;
import net.ftlines.css.scoper.ScopedFragmentResult.CssTransformationOperationType;
import net.ftlines.css.scoper.wicket.WicketPanelCssContributor;

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
	private int containerAnnotationRefCount = 0;
	private int insideDeclarationCount = 0;
	public String scope;

	private boolean debugMode;

	private CssSelectorReplace(BufferedTokenStream tokenStream, CssScopeMetadata metadata, boolean debugMode) {
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
			operationSequence.add(new CssTransformationOperation(CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT, null, "." + scope));
		}
		containerAnnotationRefCount += 1;
		rewriter.replace(ctx.start, ctx.stop, "");
	}

	

	@Override
	public void enterClassName(ClassNameContext ctx) {
		super.enterClassName(ctx);
		if (externalAnnotationRefCount == 0 && containerAnnotationRefCount == 0) {
			CssTransformationOperation op = new CssTransformationOperation(CssTransformationOperationType.CLASS_REPLACEMENT,
				ctx.getText(), "."
					+ metadata.getValue(getPropertyKey(CLASS_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ctx.getText()),
						() -> generateNewClassSelector(ctx.getText())));
			
			metadata.setValue(getPropertyKey(CLASS_CONTEXT_OPERATION_PROPERTY_FORMAT, op.getOriginalSelector()),
				op.getOperation().name());
			metadata.setValue(getPropertyKey(CLASS_CONTEXT_ORIG_PROPERTY_FORMAT, op.getOriginalSelector()),
				op.getOriginalSelector());
		
			operationSequence.add(op);
			rewriter.replace(ctx.start, ctx.stop, op.getNewSelector());
		}

	}

	@Override
	public void enterHash(HashContext ctx) {
		super.enterHash(ctx);
		if (externalAnnotationRefCount == 0 && insideDeclarationCount == 0 && containerAnnotationRefCount == 0) { 
			CssTransformationOperation op = new CssTransformationOperation(CssTransformationOperationType.ID_TO_CLASS, ctx.getText(), "."
				+ metadata.getValue(getPropertyKey(HASH_CONTEXT_COMPUTED_SCOPE_PROPERTY_FORMAT, ctx.getText()),
					() -> generateNewClassSelector(ctx.getText())));

			metadata.setValue(getPropertyKey(HASH_CONTEXT_OPERATION_PROPERTY_FORMAT, op.getOriginalSelector()),
				op.getOperation().name());
			metadata.setValue(getPropertyKey(HASH_CONTEXT_ORIG_PROPERTY_FORMAT, op.getOriginalSelector()),
				op.getOriginalSelector());

			operationSequence.add(op);
			rewriter.replace(ctx.start, ctx.stop, op.getNewSelector());
		}

	}

	@Override
	public void enterDeclarationList(DeclarationListContext ctx) {
		super.enterDeclarationList(ctx);
		insideDeclarationCount += 1;
	}

	@Override
	public void exitDeclarationList(DeclarationListContext ctx) {
		super.exitDeclarationList(ctx);
		insideDeclarationCount -= 1;
	}

	private String generateNewClassSelector(String oldSelector) {
		return scope + "_" + (debugMode ? sanitizeSelector(oldSelector) : CssScopeMetadata.generateRandomString());
	}

	private String sanitizeSelector(String oldSelector) {
		return CssScopeMetadata.generateRandomString() + "_" + oldSelector.replaceAll("[^A-Za-z0-9]", "_");
	}

	@Override
	public void enterSelectorGroup(SelectorGroupContext ctx) {
		super.enterSelectorGroup(ctx);
	}

	@Override
	public void exitKnownRuleset(KnownRulesetContext ctx) {
		super.exitKnownRuleset(ctx);
		if (externalAnnotationRefCount > 0) {
			externalAnnotationRefCount -= 1;
		}
		
		if (containerAnnotationRefCount > 0) {
			containerAnnotationRefCount -= 1;
		}
	}
	
	@Override
	public void exitUnknownRuleset(UnknownRulesetContext ctx) {
		super.exitUnknownRuleset(ctx);
		if (externalAnnotationRefCount > 0) {
			externalAnnotationRefCount -= 1;
		}
		
		if (containerAnnotationRefCount > 0) {
			containerAnnotationRefCount -= 1;
		}
	}
	
	@Override
	public void enterSelector(SelectorContext ctx) {
		super.enterSelector(ctx);
		if (externalAnnotationRefCount == 0  && containerAnnotationRefCount > 0) {
			if (isUnscopedTypeSelector(ctx)) {
				if (getOperationSequence().stream().noneMatch(this::isOuterScopeAssignement)) {
					operationSequence.add(new CssTransformationOperation(CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT,
						null, "." + scope	));
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
			} else {
				rewriter.insertBefore(ctx.start, "." + scope + " ");
			}
		}
	}
	
	@Override
	public void exitSelector(SelectorContext ctx) {
		super.exitSelector(ctx);
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
		return x.getOperation() == CssTransformationOperationType.OUTERSCOPE_ASSIGNMENT;
	}

	public List<CssTransformationOperation> getOperationSequence() {
		return operationSequence;
	}

	
	public static String getPropertyKey(String format, String selector) {
		return String.format(format, CssScopeMetadata.hashString(selector));
	}
	
	
	public static CssSelectorReplace parse(String source, CssScopeMetadata metadata, boolean debugMode) {
		try {
			
	    CodePointCharStream cs = fromString(source);	
		css3Lexer lexer = new css3Lexer(cs);
		BufferedTokenStream stream = new BufferedTokenStream(lexer);
		css3Parser par = new css3Parser(stream);
		
		CssSelectorReplace replace = new CssSelectorReplace(stream, metadata, debugMode);
		ParseTreeWalker.DEFAULT.walk(replace, par.stylesheet());
		return replace;
		} catch(Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static void main(String[] args) throws IOException {
		
		System.out.println(parse(new WicketPanelCssContributor(AbstractSourceFileModifier.pathAsString(Paths.get(
			"/Users/peter/git/harmonize/application/lms/target/classes/net/ftlines/lms/discussion/activity/classic/GradeByTypePanel.html"))).getCss().get(), 
			new CssScopeMetadata(new Properties()), true).getOutput());
	}

}
