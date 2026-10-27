package io.lexora.domain.template;

/**
 * Layout used for one customer address of a generated document.
 *
 * @param layout     name of the layout
 * @param singlePage true to force the document on a single page
 */
public record PageLayout(String layout, boolean singlePage) {

    public static final PageLayout NONE = new PageLayout(null, false);
}
