package com.opensymphony.module.sitemesh.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateTest {

    class DummyRule extends BasicRule {
        public DummyRule(String acceptableTagName) {
            super(acceptableTagName);
        }
        public void process(Tag tag) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testMapsTagNameToRule() {
        TagRule mouseRule = new DummyRule("mouse");
        TagRule donkeyRule = new DummyRule("donkey");
        TagRule lemonRule = new DummyRule("lemon");

        State state = new State();
        state.addRule(mouseRule);
        state.addRule(donkeyRule);
        state.addRule(lemonRule);

        assertSame(donkeyRule, state.getRule("donkey"));
        assertSame(lemonRule, state.getRule("lemon"));
        assertSame(mouseRule, state.getRule("mouse"));
    }

    @Test
    public void testExposesWhetherItShouldProcessATagBasedOnAvailableRules() {
        TagRule mouseRule = new DummyRule("mouse");
        TagRule donkeyRule = new DummyRule("donkey");
        TagRule lemonRule = new DummyRule("lemon");

        State state = new State();
        state.addRule(mouseRule);
        state.addRule(donkeyRule);
        state.addRule(lemonRule);

        assertTrue(state.shouldProcessTag("donkey"));
        assertTrue(state.shouldProcessTag("lemon"));
        assertFalse(state.shouldProcessTag("yeeeehaa"));
    }

    @Test
    public void testReturnsMostRecentlyAddedRuleIfMultipleMatchesFound() {
        TagRule oldRule = new DummyRule("something");
        TagRule newRule = new DummyRule("something");

        State state = new State();
        state.addRule(oldRule);
        state.addRule(newRule);

        assertSame(newRule, state.getRule("something"));
    }
}
