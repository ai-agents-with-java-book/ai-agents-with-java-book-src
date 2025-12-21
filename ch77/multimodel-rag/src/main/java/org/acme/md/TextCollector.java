package org.acme.md;


import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;

public class TextCollector extends AbstractVisitor {

    private final StringBuilder sb = new StringBuilder();

    @Override
    public void visit(Text text) {
        sb.append(text.getLiteral()).append(" ");
    }

    @Override
    public void visit(Code code) {
        sb.append(code.getLiteral()).append(" ");
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
        sb.append(" ");
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
        sb.append(" ");
    }

    public String getText() {
        return sb.toString();
    }
}
