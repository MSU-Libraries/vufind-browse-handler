package org.vufind.solr.browse.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.vufind.util.TitleNormalizer;

public class TitleNormalizerTest
{
    private TitleNormalizer titleNormalizer;

    @Before
    public void setUp()
    {
        titleNormalizer = new TitleNormalizer();
    }


    @Test
    public void sortsSimpleStrings()
    {
        assertEquals(listOf("apple", "banana", "cherry", "orange"),
                     sort(listOf("banana", "orange", "apple", "cherry")));
    }


    @Test
    public void sortsDiacriticStrings()
    {
        assertEquals(listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit", "Orange"),
                     sort(listOf("grapefruit", "Apple", "Orange", "AAA", "Äardvark", "Banana")));
    }


    @Test
    public void handlesHyphensQuotesAndWhitespace()
    {
        assertEquals(listOf("AAA", "Äardvark", "Apple", "Banana", "grapefruit",
                            "\"Hyphenated-words and double quotes\"",
                            "   inappropriate leading space",
                            "Orange"),
                     sort(listOf("Orange",
                                 "\"Hyphenated-words and double quotes\"",
                                 "Banana", "grapefruit",
                                 "   inappropriate leading space",
                                 "Äardvark", "Apple", "AAA")));

    }


    @Test
    public void sortsUnicodeCharacters()
    {
        assertEquals(listOf("apple", "바나나", "チェリー", "橙子"),
                     sort(listOf("바나나", "橙子", "apple", "チェリー")));
    }


    @Test
    public void ignoresPunctuationMixedWithSpaces()
    {
        assertArrayEquals(titleNormalizer.normalize("wharton, edith"), titleNormalizer.normalize("wharton edith"));
        assertArrayEquals(titleNormalizer.normalize("st. john"), titleNormalizer.normalize("st john"));
    }


    //
    // Helpers
    //

    private List<String> listOf(String ... args)
    {
        List<String> result = new ArrayList<String> ();
        for (String s : args) {
            result.add(s);
        }

        return result;
    }


    // http://stackoverflow.com/questions/5108091/java-comparator-for-byte-array-lexicographic
    private int compareByteArrays(byte[] left, byte[] right)
    {
        for (int i = 0, j = 0; i < left.length && j < right.length; i++, j++) {
            int a = (left[i] & 0xff);
            int b = (right[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return left.length - right.length;
    }


    private List<String> sort(List<String> list)
    {
        List<String> result = new ArrayList<String> ();
        result.addAll(list);

        Collections.sort(result, new Comparator<String> () {
            public int compare(String s1, String s2) {
                return compareByteArrays(titleNormalizer.normalize(s1),
                                         titleNormalizer.normalize(s2));
            }
        });

        return result;
    }

}
