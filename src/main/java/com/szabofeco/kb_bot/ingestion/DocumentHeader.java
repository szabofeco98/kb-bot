package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;

import java.util.Map;
import java.util.regex.Pattern;

final class DocumentHeader {

    /** docId and version are concatenated into the delete filter – guards against filter-expression injection. */
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private DocumentHeader() {
    }

    static DocumentRef parse(Map<String, String> frontMatter, String fileName, Product product) {
        String docId = requireSafe(frontMatter, "id");
        String version = requireSafe(frontMatter, "version");

        String declaredProduct = optional(frontMatter, "product");
        if (declaredProduct != null && !declaredProduct.equals(product.name())) {
            throw new InvalidDocumentException("The given product (" + product + ") does not match the one "
                    + "in the document (" + declaredProduct + ").");
        }

        String title = optional(frontMatter, "title");
        return new DocumentRef(docId, version, title == null ? fileName : title,
                optional(frontMatter, "effective_from"), product, fileName);
    }

    private static String optional(Map<String, String> frontMatter, String key) {
        String value = frontMatter.get(key);
        return value == null || value.isBlank() ? null : value;
    }

    private static String requireSafe(Map<String, String> frontMatter, String key) {
        String value = optional(frontMatter, key);
        if (value == null) {
            throw new InvalidDocumentException("Missing required frontmatter field: " + key);
        }
        if (!SAFE_VALUE.matcher(value).matches()) {
            throw new InvalidDocumentException("Invalid characters in field '" + key + "': " + value);
        }
        return value;
    }
}
