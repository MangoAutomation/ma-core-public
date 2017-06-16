/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.m2m2.rt.event.compound;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.serotonin.ShouldNeverHappenException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.AlarmLevels;
import com.serotonin.m2m2.rt.event.EventDetectorListener;
import com.serotonin.m2m2.rt.event.SimpleEventDetector;
import com.serotonin.m2m2.rt.event.type.CompoundDetectorEventType;
import com.serotonin.m2m2.rt.event.type.EventType;
import com.serotonin.m2m2.vo.event.CompoundEventDetectorVO;
import com.serotonin.util.ILifecycle;
import com.serotonin.util.LifecycleException;
import com.serotonin.web.i18n.LocalizableException;
import com.serotonin.web.i18n.LocalizableMessage;

/**
 * @author Matthew Lohbihler
 */
public class CompoundEventDetectorRT implements EventDetectorListener, ILifecycle {
    private static final char[] TOKEN_OR = { '|', '|' };
    private static final char[] TOKEN_AND = { '&', '&' };

    private final CompoundEventDetectorVO<?> vo;
    private CompoundDetectorEventType eventType;
    private LogicalOperator condition;
    private boolean currentState;

    public CompoundEventDetectorRT(CompoundEventDetectorVO<?> vo) {
        this.vo = vo;
    }

    private void raiseEvent(long time) {
    	//TODO Needs fixed
        //Common.eventManager.raiseEvent(eventType, time, vo.isReturnToNormal(), vo.getAlarmLevel(),
        //        new LocalizableMessage("event.compound.activated", vo.getName()), null);
    	throw new ShouldNeverHappenException("Unimplemented!");
    }

    private void returnToNormal(long time) {
        Common.eventManager.returnToNormal(eventType, time, AlarmLevels.URGENT); //Not a real implementation since the raiaeEvent isn't
    }

    public static LogicalOperator parseConditionStatement(String condition) throws ConditionParseException {
        if (StringUtils.isBlank(condition))
            throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.notDefined"));

        // Do bracket validation and character checking.
        int parenCount = 0;
        String allowedCharacters = "0123456789SP&|!()";
        for (int i = 0; i < condition.length(); i++) {
            char c = condition.charAt(i);
            if (allowedCharacters.indexOf(c) == -1 && !Character.isWhitespace(c))
                throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.illegalChar"),
                        i, i + 1);

            if (c == '(')
                parenCount++;
            else if (c == ')')
                parenCount--;

            if (parenCount < 0)
                throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.closeParen"), i,
                        i + 1);
        }
        if (parenCount != 0) {
            int paren = condition.indexOf('(');
            throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.openParen"), paren,
                    paren + 1);
        }

        // Maintain the original indices of the characters
        List<ConditionStatementCharacter> charList = new ArrayList<ConditionStatementCharacter>(condition.length());
        for (int i = 0; i < condition.length(); i++)
            charList.add(new ConditionStatementCharacter(condition.charAt(i), i));

        // Remove whitespace
        for (int i = condition.length() - 1; i >= 0; i--) {
            if (Character.isWhitespace(charList.get(i).c))
                charList.remove(i);
        }

        // Convert to array.
        ConditionStatementCharacter[] chars = new ConditionStatementCharacter[charList.size()];
        charList.toArray(chars);

        // Parse into tokens
        return parseTokens(chars, 0, chars.length);
    }

    private static LogicalOperator parseTokens(ConditionStatementCharacter[] chars, int from, int to)
            throws ConditionParseException {
        if (from >= chars.length)
            throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.syntax"),
                    chars[chars.length - 1].originalIndex, chars[chars.length - 1].originalIndex + 1);

        if (to - from < 0)
            throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.syntax"),
                    chars[from].originalIndex, chars[from].originalIndex + 1);

        if (to - from == 0)
            throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.syntax"),
                    chars[from].originalIndex, chars[to].originalIndex + 1);

        // Find or conditions not in parens.
        int start = indexOfLevel0(TOKEN_OR, chars, from, to);
        if (start != -1)
            // Found an or condition.
            return new OrOperator(parseTokens(chars, from, start), parseTokens(chars, start + TOKEN_OR.length, to));

        // Find and conditions not in parens.
        start = indexOfLevel0(TOKEN_AND, chars, from, to);
        if (start != -1)
            // Found an and condition.
            return new AndOperator(parseTokens(chars, from, start), parseTokens(chars, start + TOKEN_AND.length, to));

        // Check for not
        if (chars[from].c == '!')
            return new NotOperator(parseTokens(chars, from + 1, to));

        // Check for parentheses.
        if (chars[from].c == '(' && chars[to - 1].c == ')')
            return new Parenthesis(parseTokens(chars, from + 1, to - 1));

        // Must be a detector reference. Check the syntax.
        if (to - from > 1 && Character.isLetter(chars[from].c)) {
            // The rest must be digits.
            for (int i = from + 1; i < to; i++) {
                if (!Character.isDigit(chars[i].c))
                    throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.reference"),
                            chars[from].originalIndex, chars[to - 1].originalIndex + 1);
            }

            return new EventDetectorWrapper(toString(chars, from, to));
        }

        // Not good
        throw new ConditionParseException(new LocalizableMessage("compoundDetectors.validation.reference"),
                chars[from].originalIndex, chars[to - 1].originalIndex + 1);
    }

    private static int indexOfLevel0(char[] token, ConditionStatementCharacter[] chars, int from, int to) {
        int level = 0;
        boolean match;
        for (int i = from; i < to; i++) {
            if (chars[i].c == '(')
                level++;
            else if (chars[i].c == ')')
                level--;
            else if (level == 0) {
                // Try to match the token
                match = true;
                for (int j = 0; j < token.length && match; j++) {
                    if (token[j] != chars[i + j].c)
                        match = false;
                }

                if (match)
                    return i;
            }
        }
        return -1;
    }

    private static String toString(ConditionStatementCharacter[] chars, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++)
            sb.append(chars[i].c);
        return sb.toString();
    }

    public void raiseFailureEvent(LocalizableMessage message) {
    	//TODO Implement
//        SystemEventType eventType = new SystemEventType(SystemEventType.TYPE_COMPOUND_DETECTOR_FAILURE, vo.getId());
//        SystemEventType.raiseEvent(eventType, Common.timer.currentTimeMillis(), false, message);
//        vo.setDisabled(true);
//        new CompoundEventDetectorDao().saveCompoundEventDetector(vo);
        
    	throw new ShouldNeverHappenException("Unimplemented!");
    }

    //
    // / EventDetectorListener
    //
    public void eventDetectorStateChanged(long time) {
        // Evaluate the condition.
        boolean newState = condition.evaluate();

        // If it has changed, take appropriate action.
        if (newState != currentState) {
            currentState = newState;
            if (currentState)
                // Active
                raiseEvent(time);
            else
                // Inactive
                returnToNormal(time);
        }
    }

    public void eventDetectorTerminated(SimpleEventDetector source) {
    	//TODO Implement
//        Common.ctx.getRuntimeManager().stopCompoundEventDetector(vo.getId());
//        raiseFailureEvent(new LocalizableMessage("event.compound.sourceFailure", vo.getName()));
    	throw new ShouldNeverHappenException("Unimplemented!");
    }

    //
    //
    // /
    // / Lifecycle interface
    // /
    //
    //
    @Override
    public void initialize(boolean safe) throws LifecycleException {
        // Validate the condition statement.
        try {
            condition = parseConditionStatement(vo.getCondition());
        }
        catch (ConditionParseException e) {
            throw new LifecycleException(e);
        }

        try {
            condition.initialize();
        }
        catch (LocalizableException e) {
            throw new LifecycleException(e);
        }

        condition.initSource(this);

        // Create a convenience reference to the event type.
        eventType = new CompoundDetectorEventType(vo.getId());
        if (!vo.isReturnToNormal())
            eventType.setDuplicateHandling(EventType.DuplicateHandling.ALLOW);

        // Evaluate the current state.
        currentState = condition.evaluate();
        if (currentState)
            raiseEvent(Common.timer.currentTimeMillis());
        else
            returnToNormal(Common.timer.currentTimeMillis());
    }

    public void terminate() {
        if (condition != null)
            condition.terminate(this);
        returnToNormal(Common.timer.currentTimeMillis());
    }

    public void joinTermination() {
        // no op
    }

    static class ConditionStatementCharacter {
        char c;
        int originalIndex;

        public ConditionStatementCharacter(char c, int originalIndex) {
            this.c = c;
            this.originalIndex = originalIndex;
        }
    }
}
