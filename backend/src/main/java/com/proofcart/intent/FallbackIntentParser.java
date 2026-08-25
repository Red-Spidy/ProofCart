package com.proofcart.intent;

import com.proofcart.domain.IntentRules;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FallbackIntentParser {

    public IntentRules parse(String prompt) {
        String text = prompt.toLowerCase();

        // -- Budget --
        Integer maxTotalPaise = null;
        String[] budgetPatterns = {
                "(?:under|below|less than|max(?:imum)?|budget(?: of)?|within|<)\\s*(?:rs\\.?|₹|inr)?\\s*([\\d,]+)",
                "(?:rs\\.?|₹|inr)\\s*([\\d,]+)\\s*(?:or less|budget|limit|max(?:imum)?)"
        };

        for (String patternStr : budgetPatterns) {
            Pattern pattern = Pattern.compile(patternStr);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    int rupees = Integer.parseInt(matcher.group(1).replace(",", ""));
                    maxTotalPaise = rupees * 100;
                    break;
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // -- Dietary tags --
        List<String> mustHaveTags = new ArrayList<>();
        String[][] tagMap = {
                {"vegan", "vegan"},
                {"vegetarian", "vegetarian"},
                {"gluten free", "gluten-free"},
                {"gluten-free", "gluten-free"},
                {"dairy free", "dairy-free"},
                {"dairy-free", "dairy-free"},
                {"keto", "keto"},
                {"organic", "organic"},
                {"sugar free", "sugar-free"},
                {"sugar-free", "sugar-free"}
        };
        for (String[] pair : tagMap) {
            if (text.contains(pair[0]) && !mustHaveTags.contains(pair[1])) {
                mustHaveTags.add(pair[1]);
            }
        }

        // -- Allergens --
        List<String> excludedAllergens = new ArrayList<>();
        String allergenPrefixes = "no\\s+|without\\s+|free from\\s+|avoid(?:ing)?\\s+|not\\s+(?:contain(?:ing)?|have|want)\\s+";
        String[][] allergenTokens = {
                {"peanut", "peanuts"},
                {"peanuts", "peanuts"},
                {"tree nut", "tree-nuts"},
                {"tree-nut", "tree-nuts"},
                {"tree nuts", "tree-nuts"},
                {"nut", "tree-nuts"},
                {"milk", "milk"},
                {"dairy", "milk"},
                {"egg", "eggs"},
                {"eggs", "eggs"},
                {"wheat", "wheat"},
                {"gluten", "wheat"},
                {"soy", "soy"},
                {"soya", "soy"},
                {"fish", "fish"},
                {"shellfish", "shellfish"},
                {"sesame", "sesame"}
        };
        for (String[] pair : allergenTokens) {
            Pattern pattern = Pattern.compile("(?:" + allergenPrefixes + ")" + Pattern.quote(pair[0]));
            if (pattern.matcher(text).find() && !excludedAllergens.contains(pair[1])) {
                excludedAllergens.add(pair[1]);
            }
        }

        // -- Delivery --
        Object deliveryRequirement = null;
        if (Pattern.compile("\\btoday\\b|\\bsame.?day\\b|\\bimmediate\\b|\\bnow\\b").matcher(text).find()) {
            deliveryRequirement = "today";
        } else if (Pattern.compile("\\btomorrow\\b|\\bnext day\\b|\\b1.?day\\b").matcher(text).find()) {
            deliveryRequirement = "tomorrow";
        } else {
            Matcher m = Pattern.compile("within\\s+(\\d+)\\s*days?").matcher(text);
            if (m.find()) {
                deliveryRequirement = Integer.parseInt(m.group(1));
            }
        }

        // -- Returnability --
        boolean mustBeReturnable = Pattern.compile(
                "\\breturnable\\b|\\bcan\\s+(?:be\\s+)?return|\\breturn(?:s)?\\s+(?:allowed|accepted|ok|policy)\\b|\\breturns\\s+(?:are\\s+)?(?:allowed|accepted|ok)\\b"
        ).matcher(text).find();

        // -- Subscription --
        boolean subscriptionAllowed = !Pattern.compile(
                "\\bno\\s+subscription\\b|\\bnot\\s+a\\s+subscription\\b|\\bone.?time\\b|\\bsingle\\s+(?:order|purchase)\\b|\\bnon.?subscription\\b"
        ).matcher(text).find();

        // -- Clarification --
        boolean hasAnything = maxTotalPaise != null || !mustHaveTags.isEmpty() || !excludedAllergens.isEmpty() ||
                deliveryRequirement != null || !subscriptionAllowed || mustBeReturnable;

        boolean needsClarification = !hasAnything && text.trim().length() < 20;
        String clarificationQuestion = needsClarification ? "Could you share a bit more detail? For example: budget, dietary preferences, delivery time, or allergens to avoid." : null;

        return new IntentRules(
                maxTotalPaise,
                mustHaveTags,
                excludedAllergens,
                deliveryRequirement,
                subscriptionAllowed,
                mustBeReturnable,
                needsClarification,
                clarificationQuestion,
                0.8
        );
    }
}
