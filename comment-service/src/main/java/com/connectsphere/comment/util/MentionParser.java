package com.connectsphere.comment.util;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MentionParser — extracts all @username tokens from a piece of text.
 *
 * Rules:
 *   - A mention starts with '@' followed by alphanumeric characters or underscores.
 *   - The '@' must not be preceded by another alphanumeric character
 *     (prevents matching email addresses like user@domain.com).
 *   - Returns unique usernames (duplicates removed).
 *
 * Example:
 *   input:  "Hey @john and @jane, check this out! @john you too."
 *   output: ["john", "jane"]
 */
public class MentionParser {

    // (?<![\\w]) — negative lookbehind: '@' must not follow a word character
    // @([\\w]+)  — capture group: one or more word characters after '@'
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("(?<![\\w])@([\\w]+)");

    /**
     * Extract unique @mentioned usernames from the given text.
     * Returns an empty list if text is null or contains no mentions.
     */
    public static List<String> extractMentions(String text) {
        List<String> mentions = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return mentions;
        }

        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (!mentions.contains(username)) {   // deduplicate
                mentions.add(username);
            }
        }
        return mentions;
    }
}
