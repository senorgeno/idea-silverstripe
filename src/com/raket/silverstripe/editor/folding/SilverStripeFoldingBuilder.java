package com.raket.silverstripe.editor.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.raket.silverstripe.psi.SilverStripeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SilverStripeFoldingBuilder implements FoldingBuilder, DumbAware {

	@NotNull
	@Override
	public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
		List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
		appendDescriptors(node.getPsi(), descriptors, document);
		return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
	}

	private void appendDescriptors(PsiElement psi, List<FoldingDescriptor> descriptors, Document document) {
		ASTNode node = psi.getNode();
		if (node == null || isSingleLine(psi, document)) {
			return;
		}

		if (SilverStripeTypes.SS_BLOCK_STATEMENT == node.getElementType()) {

			ASTNode endOpenBlockStache = getOpenBlockCloseStacheElement(node.getFirstChildNode());
			ASTNode endCloseBlockStache = getCloseBlockCloseStacheElement(node.getLastChildNode());

			// if we've got a well formed block with the open and close elems we need, define a region to fold
			if (endOpenBlockStache != null && endCloseBlockStache != null) {
				int endOfFirstOpenStacheLine
						= document.getLineEndOffset(document.getLineNumber(node.getTextRange().getStartOffset()));

				// we set the start of the text we'll fold to be just before the close braces of the open stache,
				//     or, if the open stache spans multiple lines, to the end of the first line
				int foldingRangeStartOffset = Math.min(endOpenBlockStache.getTextRange().getStartOffset(), endOfFirstOpenStacheLine);
				// we set the end of the text we'll fold to be just before the final close braces in this block
				int foldingRangeEndOffset = endCloseBlockStache.getTextRange().getStartOffset();

				TextRange range = new TextRange(foldingRangeStartOffset, foldingRangeEndOffset);

				descriptors.add(new FoldingDescriptor(node, range));
			}
		}

		PsiElement child = psi.getFirstChild();
		while (child != null) {
			appendDescriptors(child, descriptors, document);
			child = child.getNextSibling();
		}
	}

	private ASTNode getOpenBlockCloseStacheElement(ASTNode node) {
		ASTNode contentNode = node.getFirstChildNode();
		if (contentNode == null || contentNode.getElementType() != SilverStripeTypes.SS_BLOCK_START_STATEMENT) {
			return null;
		}

		ASTNode endOpenStache = contentNode.getLastChildNode();
		if (endOpenStache == null || endOpenStache.getElementType() != SilverStripeTypes.SS_BLOCK_END) {
			return null;
		}

		return endOpenStache;
	}

	private ASTNode getCloseBlockCloseStacheElement(ASTNode node) {
		ASTNode contentNode = node.getFirstChildNode();
		if (contentNode == null || contentNode.getElementType() != SilverStripeTypes.SS_BLOCK_END_STATEMENT) {
			return null;
		}

		ASTNode endCloseStache = contentNode.getLastChildNode();
		if (endCloseStache == null || endCloseStache.getElementType() != SilverStripeTypes.SS_BLOCK_END) {
			return null;
		}

		return endCloseStache;
	}

	@Nullable
	@Override
	public String getPlaceholderText(@NotNull ASTNode node) {
		return "...";
	}

	@Override
	public boolean isCollapsedByDefault(@NotNull ASTNode node) {
		return false;
	}

	/**
	 * Return true if this psi element does not span more than one line in the given document
	 */
	private static boolean isSingleLine(PsiElement element, Document document) {
		TextRange range = element.getTextRange();
		return document.getLineNumber(range.getStartOffset()) == document.getLineNumber(range.getEndOffset());
	}
}