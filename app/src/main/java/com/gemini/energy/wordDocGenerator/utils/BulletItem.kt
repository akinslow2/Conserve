package com.gemini.energy.wordDocGenerator.utils

/**
 * object to create bulleted list with inline text styling
 */
class BulletItem(
        /**
         * collection of text of list item with styling notes
         */
        val components: Array<Span>
) {


    /**
     * part (or all) of a line in a bulleted list
     */
    class Span(
            /**
             * partial text of the line
             */
            val text: String,

            /**
             * if this part of the text should be italic
             */
            val isItalic: Boolean = false
    )
}