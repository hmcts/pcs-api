package uk.gov.hmcts.reform.pcs.ccd.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RichTextFormatterTest {

    @Test
    void keepsPlainTextAsItIs() {
        String wording = "1. The hearing is adjourned.\n2. Costs reserved.";

        assertThat(RichTextFormatter.toPlainText(wording)).isEqualTo(wording);
        assertThat(RichTextFormatter.toPlainText(null)).isNull();
    }

    @Test
    void numbersAListAsTheApplicantSawIt() {
        String html = "<p>The claimant asks that:</p><ol><li><p>The hearing is <strong>adjourned</strong>.</p></li>"
            + "<li><p>Unless the defendant complies with paragraph 1, costs are reserved.</p></li></ol>";

        assertThat(RichTextFormatter.toPlainText(html)).isEqualTo("""
            The claimant asks that:
            1. The hearing is adjourned.
            2. Unless the defendant complies with paragraph 1, costs are reserved.""");
    }

    @Test
    void keepsAListsStartStyleAndNesting() {
        String html = "<ol start=\"3\"><li><p>Parent</p><ol type=\"a\"><li><p>First</p></li><li><p>Second</p></li>"
            + "</ol></li></ol><ul><li><p>Bullet</p></li></ul>";

        assertThat(RichTextFormatter.toPlainText(html)).isEqualTo("""
            3. Parent
                a. First
                b. Second
            • Bullet""");
    }

    @Test
    void endsALineAtALineBreakAndIgnoresAListStartItCannotRead() {
        String html = "<hr><p>First line<br>second line</p><ol start=\"\"><li><p>Clause</p></li></ol>";

        assertThat(RichTextFormatter.toPlainText(html)).isEqualTo("""
            First line
            second line
            1. Clause""");
    }
}
