package com.example.cooking.ui.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownUtils {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((https?://[^\\s)]+)\\)");
    private static final Pattern BOLD_ASTERISK_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern BOLD_UNDERSCORE_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern ITALIC_ASTERISK_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BULLET_PATTERN = Pattern.compile("(?m)^(\\s*)[-*]\\s+");

    private MarkdownUtils() {
    }

    public static CharSequence toSpannable(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(markdown);
        replaceBulletMarkers(builder);
        applyLinks(builder);
        applyInlineCode(builder);
        applyStyle(builder, BOLD_ASTERISK_PATTERN, Typeface.BOLD);
        applyStyle(builder, BOLD_UNDERSCORE_PATTERN, Typeface.BOLD);
        applyStyle(builder, ITALIC_ASTERISK_PATTERN, Typeface.ITALIC);
        applyStyle(builder, ITALIC_UNDERSCORE_PATTERN, Typeface.ITALIC);
        return builder;
    }

    private static void replaceBulletMarkers(SpannableStringBuilder builder) {
        List<int[]> matches = new ArrayList<>();
        Matcher matcher = BULLET_PATTERN.matcher(builder);
        while (matcher.find()) {
            matches.add(new int[]{matcher.start(), matcher.end(), matcher.group(1).length()});
        }

        for (int index = matches.size() - 1; index >= 0; index--) {
            int[] match = matches.get(index);
            String indent = builder.subSequence(match[0], match[0] + match[2]).toString();
            builder.replace(match[0], match[1], indent + "• ");
        }
    }

    private static void applyLinks(SpannableStringBuilder builder) {
        List<LinkMatch> matches = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(builder);
        while (matcher.find()) {
            matches.add(new LinkMatch(matcher.start(), matcher.end(), matcher.group(1), matcher.group(2)));
        }

        for (int index = matches.size() - 1; index >= 0; index--) {
            LinkMatch match = matches.get(index);
            builder.replace(match.start, match.end, match.text);
            builder.setSpan(
                    new URLSpan(match.url),
                    match.start,
                    match.start + match.text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private static void applyInlineCode(SpannableStringBuilder builder) {
        List<SimpleMatch> matches = collectMatches(builder, INLINE_CODE_PATTERN);
        for (int index = matches.size() - 1; index >= 0; index--) {
            SimpleMatch match = matches.get(index);
            builder.replace(match.start, match.end, match.content);
            int spanEnd = match.start + match.content.length();
            builder.setSpan(new TypefaceSpan("monospace"), match.start, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new BackgroundColorSpan(0x14000000), match.start, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static void applyStyle(SpannableStringBuilder builder, Pattern pattern, int style) {
        List<SimpleMatch> matches = collectMatches(builder, pattern);
        for (int index = matches.size() - 1; index >= 0; index--) {
            SimpleMatch match = matches.get(index);
            builder.replace(match.start, match.end, match.content);
            builder.setSpan(
                    new StyleSpan(style),
                    match.start,
                    match.start + match.content.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private static List<SimpleMatch> collectMatches(SpannableStringBuilder builder, Pattern pattern) {
        List<SimpleMatch> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(builder);
        while (matcher.find()) {
            matches.add(new SimpleMatch(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return matches;
    }

    private static final class SimpleMatch {
        private final int start;
        private final int end;
        private final String content;

        private SimpleMatch(int start, int end, String content) {
            this.start = start;
            this.end = end;
            this.content = content;
        }
    }

    private static final class LinkMatch {
        private final int start;
        private final int end;
        private final String text;
        private final String url;

        private LinkMatch(int start, int end, String text, String url) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.url = url;
        }
    }
}