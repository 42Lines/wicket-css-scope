package net.ftlines.css.scoper;

import static org.antlr.v4.runtime.CharStreams.fromString;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import antlr.css3.css3Lexer;
import antlr.css3.css3Parser;
import net.ftlines.css.scoper.CssSelectorReplace.CssTransformationOperation;
import net.ftlines.css.scoper.CssSelectorReplace.CssTransformationOperationType;

public class ScopedFragmentResult {

	private CssScopeMetadata metadata;

	private String oldCss;
	private String oldHtml;

	private String newCss;
	private String newHtml;

	private ScopedFragmentResult() {
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

	public static ScopedFragmentResult transform(String css, String markup, CssScopeMetadata metadata) {
		CodePointCharStream cs = fromString(css);

		css3Lexer lexer = new css3Lexer(cs);
		BufferedTokenStream stream = new BufferedTokenStream(lexer);
		css3Parser par = new css3Parser(stream);
		CssSelectorReplace replace = new CssSelectorReplace(stream, metadata);
		ParseTreeWalker.DEFAULT.walk(replace, par.stylesheet());

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

		ScopedFragmentResult r = new ScopedFragmentResult();
		r.metadata = metadata;

		r.oldCss = css;
		r.oldHtml = markup;

		r.newCss = replace.getOutput();
		r.newHtml = doc.body().html().toString();

		return r;
	}

}
