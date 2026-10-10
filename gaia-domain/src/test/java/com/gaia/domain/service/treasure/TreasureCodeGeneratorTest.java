package com.gaia.domain.service.treasure;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 夺宝码生成器测试。
 */
class TreasureCodeGeneratorTest {

    private final TreasureCodeGenerator generator = new TreasureCodeGenerator();

    @Test
    void testGenerateOne_isSixUppercaseLetters() {
        for (int i = 0; i < 200; i++) {
            String code = generator.generateOne();
            assertEquals(6, code.length(), "code length must be 6");
            for (int j = 0; j < code.length(); j++) {
                char c = code.charAt(j);
                assertTrue(c >= 'A' && c <= 'Z', "char must be A-Z: " + c);
            }
        }
    }

    @Test
    void testIsValid() {
        assertTrue(generator.isValid("ABCDEF"));
        assertFalse(generator.isValid("ABCDE"));
        assertFalse(generator.isValid("ABCDEFG"));
        assertFalse(generator.isValid("abc123"));
        assertFalse(generator.isValid(null));
        assertFalse(generator.isValid(""));
        assertFalse(generator.isValid("ABCD F"));
    }

    @Test
    void testGenerateBatch1000_noCollision() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String code = generator.generateOne();
            assertTrue(seen.add(code), "unexpected collision at i=" + i + " code=" + code);
        }
        assertEquals(1000, seen.size());
    }
}