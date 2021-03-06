/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.analysis;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;


/**
 * 
 */
public class TestCapitalizationFilterFactory extends BaseTokenStreamTestCase {
  
  public void testCapitalization() throws Exception 
  {
    Map<String,String> args = new HashMap<String, String>();
    args.put( CapitalizationFilterFactory.KEEP, "and the it BIG" );
    args.put( CapitalizationFilterFactory.ONLY_FIRST_WORD, "true" );  
    
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init( args );
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("kiTTEN"), MockTokenizer.WHITESPACE, false)),
        new String[] { "Kitten" });
    
    factory.forceFirstLetter = true;

    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("and"), MockTokenizer.WHITESPACE, false)),
        new String[] { "And" });

    //first is forced, but it's not a keep word, either
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("AnD"), MockTokenizer.WHITESPACE, false)),
        new String[] { "And" });

    factory.forceFirstLetter = false;

    //first is not forced, but it's not a keep word, either
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("AnD"), MockTokenizer.WHITESPACE, false)),
        new String[] { "And" });

    factory.forceFirstLetter = true;
    
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("big"), MockTokenizer.WHITESPACE, false)),
        new String[] { "Big" });
    
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("BIG"), MockTokenizer.WHITESPACE, false)),
        new String[] { "BIG" });

    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("Hello thEre my Name is Ryan"), MockTokenizer.KEYWORD, false)),
        new String[] { "Hello there my name is ryan" });
        
    // now each token
    factory.onlyFirstWord = false;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("Hello thEre my Name is Ryan"), MockTokenizer.WHITESPACE, false)),
        new String[] { "Hello", "There", "My", "Name", "Is", "Ryan" });
    
    // now only the long words
    factory.minWordLength = 3;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("Hello thEre my Name is Ryan"), MockTokenizer.WHITESPACE, false)),
        new String[] { "Hello", "There", "my", "Name", "is", "Ryan" });
    
    // without prefix
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("McKinley"), MockTokenizer.WHITESPACE, false)),
        new String[] { "Mckinley" });
    
    // Now try some prefixes
    factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    args.put( "okPrefix", "McK" );  // all words
    factory.init( args );
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("McKinley"), MockTokenizer.WHITESPACE, false)),
        new String[] { "McKinley" });
    
    // now try some stuff with numbers
    factory.forceFirstLetter = false;
    factory.onlyFirstWord = false;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("1st 2nd third"), MockTokenizer.WHITESPACE, false)),
        new String[] { "1st", "2nd", "Third" });
    
    factory.forceFirstLetter = true;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("the The the"), MockTokenizer.KEYWORD, false)),
        new String[] { "The The the" });
  }

  public void testKeepIgnoreCase() throws Exception {
    Map<String,String> args = new HashMap<String, String>();
    args.put( CapitalizationFilterFactory.KEEP, "kitten" );
    args.put( CapitalizationFilterFactory.KEEP_IGNORE_CASE, "true" );
    args.put( CapitalizationFilterFactory.ONLY_FIRST_WORD, "true" );

    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init( args );
    factory.forceFirstLetter = true;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("kiTTEN"), MockTokenizer.KEYWORD, false)),
        new String[] { "KiTTEN" });

    factory.forceFirstLetter = false;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("kiTTEN"), MockTokenizer.KEYWORD, false)),
        new String[] { "kiTTEN" });

    factory.keep = null;
    assertTokenStreamContents(factory.create(
        new MockTokenizer(new StringReader("kiTTEN"), MockTokenizer.KEYWORD, false)),
        new String[] { "Kitten" });
  }
  
  /**
   * Test CapitalizationFilterFactory's minWordLength option.
   * 
   * This is very weird when combined with ONLY_FIRST_WORD!!!
   */
  public void testMinWordLength() throws Exception {
    Map<String,String> args = new HashMap<String,String>();
    args.put(CapitalizationFilterFactory.ONLY_FIRST_WORD, "true");
    args.put(CapitalizationFilterFactory.MIN_WORD_LENGTH, "5");
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init(args);
    Tokenizer tokenizer = new MockTokenizer(new StringReader(
        "helo testing"), MockTokenizer.WHITESPACE, false);
    TokenStream ts = factory.create(tokenizer);
    assertTokenStreamContents(ts, new String[] {"helo", "Testing"});
  }
  
  /**
   * Test CapitalizationFilterFactory's maxWordCount option with only words of 1
   * in each token (it should do nothing)
   */
  public void testMaxWordCount() throws Exception {
    Map<String,String> args = new HashMap<String,String>();
    args.put(CapitalizationFilterFactory.MAX_WORD_COUNT, "2");
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init(args);
    Tokenizer tokenizer = new MockTokenizer(new StringReader(
        "one two three four"), MockTokenizer.WHITESPACE, false);
    TokenStream ts = factory.create(tokenizer);
    assertTokenStreamContents(ts, new String[] {"One", "Two", "Three", "Four"});
  }
  
  /**
   * Test CapitalizationFilterFactory's maxWordCount option when exceeded
   */
  public void testMaxWordCount2() throws Exception {
    Map<String,String> args = new HashMap<String,String>();
    args.put(CapitalizationFilterFactory.MAX_WORD_COUNT, "2");
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init(args);
    Tokenizer tokenizer = new MockTokenizer(new StringReader(
        "one two three four"), MockTokenizer.KEYWORD, false);
    TokenStream ts = factory.create(tokenizer);
    assertTokenStreamContents(ts, new String[] {"one two three four"});
  }
  
  /**
   * Test CapitalizationFilterFactory's maxTokenLength option when exceeded
   * 
   * This is weird, it is not really a max, but inclusive (look at 'is')
   */
  public void testMaxTokenLength() throws Exception {
    Map<String,String> args = new HashMap<String,String>();
    args.put(CapitalizationFilterFactory.MAX_TOKEN_LENGTH, "2");
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init(args);
    Tokenizer tokenizer = new MockTokenizer(new StringReader(
        "this is a test"), MockTokenizer.WHITESPACE, false);
    TokenStream ts = factory.create(tokenizer);
    assertTokenStreamContents(ts, new String[] {"this", "is", "A", "test"});
  }
  
  /**
   * Test CapitalizationFilterFactory's forceFirstLetter option
   */
  public void testForceFirstLetter() throws Exception {
    Map<String,String> args = new HashMap<String,String>();
    args.put(CapitalizationFilterFactory.KEEP, "kitten");
    args.put(CapitalizationFilterFactory.FORCE_FIRST_LETTER, "true");
    CapitalizationFilterFactory factory = new CapitalizationFilterFactory();
    factory.setLuceneMatchVersion(TEST_VERSION_CURRENT);
    factory.init(args);
    Tokenizer tokenizer = new MockTokenizer(new StringReader("kitten"), MockTokenizer.WHITESPACE, false);
    TokenStream ts = factory.create(tokenizer);
    assertTokenStreamContents(ts, new String[] {"Kitten"});
  }
}
