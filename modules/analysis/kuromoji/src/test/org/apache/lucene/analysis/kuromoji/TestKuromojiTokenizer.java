package org.apache.lucene.analysis.kuromoji;

/**
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.kuromoji.Segmenter.Mode;
import org.apache.lucene.analysis.kuromoji.dict.UserDictionary;
import org.apache.lucene.analysis.kuromoji.tokenattributes.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.UnicodeUtil;
import org.apache.lucene.util._TestUtil;

public class TestKuromojiTokenizer extends BaseTokenStreamTestCase {

  private static UserDictionary readDict() {
    InputStream is = SegmenterTest.class.getResourceAsStream("userdict.txt");
    if (is == null) {
      throw new RuntimeException("Cannot find userdict.txt in test classpath!");
    }
    try {
      try {
        Reader reader = new InputStreamReader(is, IOUtils.CHARSET_UTF_8);
        return new UserDictionary(reader);
      } finally {
        is.close();
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new KuromojiTokenizer2(reader, readDict(), false, Mode.SEARCH);
      return new TokenStreamComponents(tokenizer, tokenizer);
    }
  };

  private Analyzer analyzerNoPunct = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new KuromojiTokenizer2(reader, readDict(), true, Mode.SEARCH);
      return new TokenStreamComponents(tokenizer, tokenizer);
    }
  };

  private Analyzer analyzerWithCompounds = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new KuromojiTokenizer2(reader, readDict(), false, Mode.SEARCH_WITH_COMPOUNDS);
      return new TokenStreamComponents(tokenizer, tokenizer);
    }
  };
  
  private Analyzer extendedModeAnalyzerNoPunct = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new KuromojiTokenizer2(reader, readDict(), true, Mode.EXTENDED);
      return new TokenStreamComponents(tokenizer, tokenizer);
    }
  };
  
  public void testDecomposition1() throws Exception {
    assertAnalyzesTo(analyzerNoPunct, "本来は、貧困層の女性や子供に医療保護を提供するために創設された制度である、" +
                         "アメリカ低所得者医療援助制度が、今日では、その予算の約３分の１を老人に費やしている。",
     new String[] { "本来", "は",  "貧困", "層", "の", "女性", "や", "子供", "に", "医療", "保護", "を",      
                    "提供", "する", "ため", "に", "創設", "さ", "れ", "た", "制度", "で", "ある",  "アメリカ", 
                    "低", "所得", "者", "医療", "援助", "制度", "が",  "今日", "で", "は",  "その",
                    "予算", "の", "約", "３", "分の", "１", "を", "老人", "に", "費やし", "て", "いる" },
     new int[] { 0, 2, 4, 6, 7,  8, 10, 11, 13, 14, 16, 18, 19, 21, 23, 25, 26, 28, 29, 30, 
                 31, 33, 34, 37, 41, 42, 44, 45, 47, 49, 51, 53, 55, 56, 58, 60,
                 62, 63, 64, 65, 67, 68, 69, 71, 72, 75, 76 },
     new int[] { 2, 3, 6, 7, 8, 10, 11, 13, 14, 16, 18, 19, 21, 23, 25, 26, 28, 29, 30, 31,
                 33, 34, 36, 41, 42, 44, 45, 47, 49, 51, 52, 55, 56, 57, 60, 62,
                 63, 64, 65, 67, 68, 69, 71, 72, 75, 76, 78 }
    );
  }
  
  public void testDecomposition2() throws Exception {
    assertAnalyzesTo(analyzerNoPunct, "麻薬の密売は根こそぎ絶やさなければならない",
      new String[] { "麻薬", "の", "密売", "は", "根こそぎ", "絶やさ", "なけれ", "ば", "なら", "ない" },
      new int[] { 0, 2, 3, 5, 6,  10, 13, 16, 17, 19 },
      new int[] { 2, 3, 5, 6, 10, 13, 16, 17, 19, 21 }
    );
  }
  
  public void testDecomposition3() throws Exception {
    assertAnalyzesTo(analyzerNoPunct, "魔女狩大将マシュー・ホプキンス。",
      new String[] { "魔女", "狩", "大将", "マシュー",  "ホプキンス" },
      new int[] { 0, 2, 3, 5, 10 },
      new int[] { 2, 3, 5, 9, 15 }
    );
  }

  public void testDecomposition4() throws Exception {
    assertAnalyzesTo(analyzer, "これは本ではない",
      new String[] { "これ", "は", "本", "で", "は", "ない" },
      new int[] { 0, 2, 3, 4, 5, 6 },
      new int[] { 2, 3, 4, 5, 6, 8 }
    );
  }

  /* Note this is really a stupid test just to see if things arent horribly slow.
   * ideally the test would actually fail instead of hanging...
   */
  public void testDecomposition5() throws Exception {
    TokenStream ts = analyzer.tokenStream("bogus", new StringReader("くよくよくよくよくよくよくよくよくよくよくよくよくよくよくよくよくよくよくよくよ"));
    ts.reset();
    while (ts.incrementToken()) {
      
    }
    ts.end();
    ts.close();
  }

  /*
    // NOTE: intentionally fails!  Just trying to debug this
    // one input...
  public void testDecomposition6() throws Exception {
    assertAnalyzesTo(analyzer, "奈良先端科学技術大学院大学",
      new String[] { "これ", "は", "本", "で", "は", "ない" },
      new int[] { 0, 2, 3, 4, 5, 6 },
      new int[] { 2, 3, 4, 5, 6, 8 }
                     );
  }
  */

  /** Tests that sentence offset is incorporated into the resulting offsets */
  public void testTwoSentences() throws Exception {
    assertAnalyzesTo(analyzerNoPunct, "魔女狩大将マシュー・ホプキンス。 魔女狩大将マシュー・ホプキンス。",
      new String[] { "魔女", "狩", "大将", "マシュー", "ホプキンス",  "魔女", "狩", "大将", "マシュー",  "ホプキンス"  },
      new int[] { 0, 2, 3, 5, 10, 17, 19, 20, 22, 27 },
      new int[] { 2, 3, 5, 9, 15, 19, 20, 22, 26, 32 }
    );
  }

  /** blast some random strings through the analyzer */
  public void testRandomStrings() throws Exception {
    checkRandomData(random, analyzer, 10000*RANDOM_MULTIPLIER);
  }
  
  public void testLargeDocReliability() throws Exception {
    for (int i = 0; i < 100; i++) {
      String s = _TestUtil.randomUnicodeString(random, 10000);
      TokenStream ts = analyzer.tokenStream("foo", new StringReader(s));
      ts.reset();
      while (ts.incrementToken()) {
      }
    }
  }
  
  /** simple test for supplementary characters */
  public void testSurrogates() throws IOException {
    assertAnalyzesTo(analyzer, "𩬅艱鍟䇹愯瀛",
      new String[] { "𩬅", "艱", "鍟", "䇹", "愯", "瀛" });
  }
  
  /** random test ensuring we don't ever split supplementaries */
  public void testSurrogates2() throws IOException {
    int numIterations = atLeast(10000);
    for (int i = 0; i < numIterations; i++) {
      if (VERBOSE) {
        System.out.println("\nTEST: iter=" + i);
      }
      String s = _TestUtil.randomUnicodeString(random, 100);
      TokenStream ts = analyzer.tokenStream("foo", new StringReader(s));
      CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
      ts.reset();
      while (ts.incrementToken()) {
        assertTrue(UnicodeUtil.validUTF16String(termAtt));
      }
    }
  }

  public void testOnlyPunctuation() throws IOException {
    TokenStream ts = analyzerNoPunct.tokenStream("foo", new StringReader("。、。。"));
    ts.reset();
    assertFalse(ts.incrementToken());
    ts.end();
  }

  public void testOnlyPunctuationExtended() throws IOException {
    TokenStream ts = extendedModeAnalyzerNoPunct.tokenStream("foo", new StringReader("......"));
    ts.reset();
    assertFalse(ts.incrementToken());
    ts.end();
  }
  
  // note: test is kinda silly since kuromoji emits punctuation tokens.
  // but, when/if we filter these out it will be useful.
  public void testEnd() throws Exception {
    assertTokenStreamContents(analyzerNoPunct.tokenStream("foo", new StringReader("これは本ではない")),
        new String[] { "これ", "は", "本", "で", "は", "ない" },
        new int[] { 0, 2, 3, 4, 5, 6 },
        new int[] { 2, 3, 4, 5, 6, 8 },
        new Integer(8)
    );
    
    assertTokenStreamContents(analyzerNoPunct.tokenStream("foo", new StringReader("これは本ではない    ")),
        new String[] { "これ", "は", "本", "で", "は", "ない"  },
        new int[] { 0, 2, 3, 4, 5, 6, 8 },
        new int[] { 2, 3, 4, 5, 6, 8, 9 },
        new Integer(12)
    );
  }

  public void testUserDict() throws Exception {
    // Not a great test because w/o userdict.txt the
    // segmentation is the same:
    assertTokenStreamContents(analyzer.tokenStream("foo", new StringReader("関西国際空港に行った")),
                              new String[] { "関西", "国際", "空港", "に", "行っ", "た"  },
                              new int[] { 0, 2, 4, 6, 7, 9 },
                              new int[] { 2, 4, 6, 7, 9, 10 },
                              new Integer(10)
    );
  }

  public void testUserDict2() throws Exception {
    // Better test: w/o userdict the segmentation is different:
    assertTokenStreamContents(analyzer.tokenStream("foo", new StringReader("朝青龍")),
                              new String[] { "朝青龍"  },
                              new int[] { 0 },
                              new int[] { 3 },
                              new Integer(3)
    );
  }
  
  public void testSegmentation() throws Exception {
    // Skip tests for Michelle Kwan -- UniDic segments Kwan as ク ワン
    //		String input = "ミシェル・クワンが優勝しました。スペースステーションに行きます。うたがわしい。";
    //		String[] surfaceForms = {
    //				"ミシェル", "・", "クワン", "が", "優勝", "し", "まし", "た", "。",
    //				"スペース", "ステーション", "に", "行き", "ます", "。",
    //				"うたがわしい", "。"
    //		};
    String input = "スペースステーションに行きます。うたがわしい。";
    String[] surfaceForms = {
        "スペース", "ステーション", "に", "行き", "ます", "。",
        "うたがわしい", "。"
    };
    assertAnalyzesTo(analyzer,
                     input,
                     surfaceForms);
    assertAnalyzesTo(analyzerWithCompounds,
                     input,
                     surfaceForms);
  }

  private void assertReadings(String input, String... readings) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    ReadingAttribute readingAtt = ts.addAttribute(ReadingAttribute.class);
    ts.reset();
    for(String reading : readings) {
      assertTrue(ts.incrementToken());
      assertEquals(reading, readingAtt.getReading());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }

  private void assertPronunciations(String input, String... pronunciations) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    ReadingAttribute readingAtt = ts.addAttribute(ReadingAttribute.class);
    ts.reset();
    for(String pronunciation : pronunciations) {
      assertTrue(ts.incrementToken());
      assertEquals(pronunciation, readingAtt.getPronunciation());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }
  
  private void assertBaseForms(String input, String... baseForms) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    BaseFormAttribute baseFormAtt = ts.addAttribute(BaseFormAttribute.class);
    ts.reset();
    for(String baseForm : baseForms) {
      assertTrue(ts.incrementToken());
      assertEquals(baseForm, baseFormAtt.getBaseForm());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }

  private void assertInflectionTypes(String input, String... inflectionTypes) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    InflectionAttribute inflectionAtt = ts.addAttribute(InflectionAttribute.class);
    ts.reset();
    for(String inflectionType : inflectionTypes) {
      assertTrue(ts.incrementToken());
      assertEquals(inflectionType, inflectionAtt.getInflectionType());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }

  private void assertInflectionForms(String input, String... inflectionForms) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    InflectionAttribute inflectionAtt = ts.addAttribute(InflectionAttribute.class);
    ts.reset();
    for(String inflectionForm : inflectionForms) {
      assertTrue(ts.incrementToken());
      assertEquals(inflectionForm, inflectionAtt.getInflectionForm());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }
  
  private void assertPartsOfSpeech(String input, String... partsOfSpeech) throws IOException {
    TokenStream ts = analyzer.tokenStream("ignored", new StringReader(input));
    PartOfSpeechAttribute partOfSpeechAtt = ts.addAttribute(PartOfSpeechAttribute.class);
    ts.reset();
    for(String partOfSpeech : partsOfSpeech) {
      assertTrue(ts.incrementToken());
      assertEquals(partOfSpeech, partOfSpeechAtt.getPartOfSpeech());
    }
    assertFalse(ts.incrementToken());
    ts.end();
  }
  
  public void testReadings() throws Exception {
    assertReadings("寿司が食べたいです。",
                   "スシ",
                   "ガ",
                   "タベ",
                   "タイ",
                   "デス",
                   "。");
  }
  
  public void testReadings2() throws Exception {
    assertReadings("多くの学生が試験に落ちた。",
                   "オオク",
                   "ノ",
                   "ガクセイ",
                   "ガ",
                   "シケン",
                   "ニ",
                   "オチ",
                   "タ",
                   "。");
  }
  
  public void testPronunciations() throws Exception {
    assertPronunciations("寿司が食べたいです。",
                         "スシ",
                         "ガ",
                         "タベ",
                         "タイ",
                         "デス",
                         "。");
  }
  
  public void testPronunciations2() throws Exception {
    // pronunciation differs from reading here
    assertPronunciations("多くの学生が試験に落ちた。",
                         "オーク",
                         "ノ",
                         "ガクセイ",
                         "ガ",
                         "シケン",
                         "ニ",
                         "オチ",
                         "タ",
                         "。");
  }
  
  public void testBasicForms() throws Exception {
    assertBaseForms("それはまだ実験段階にあります。",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "ある",
                    null,
                    null);
  }
  
  public void testInflectionTypes() throws Exception {
    assertInflectionTypes("それはまだ実験段階にあります。",
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          "五段・ラ行",
                          "特殊・マス",
                          null);
  }
  
  public void testInflectionForms() throws Exception {
    assertInflectionForms("それはまだ実験段階にあります。",
                          null,
                          null,
                          null,
                          null,
                          null,
                          null,
                          "連用形",
                          "基本形",
                          null);
  }
  
  public void testPartOfSpeech() throws Exception {
    assertPartsOfSpeech("それはまだ実験段階にあります。",
                        "名詞-代名詞-一般",
                        "助詞-係助詞",
                        "副詞-助詞類接続",
                        "名詞-サ変接続",
                        "名詞-一般",
                        "助詞-格助詞-一般",
                        "動詞-自立",
                        "助動詞",
                        "記号-句点");
  }

  // TODO: the next 2 tests are no longer using the first/last word ids, maybe lookup the words and fix?
  // do we have a possibility to actually lookup the first and last word from dictionary?
  public void testYabottai() throws Exception {
    assertAnalyzesTo(analyzer, "やぼったい",
                     new String[] {"やぼったい"});
    assertAnalyzesTo(analyzerWithCompounds, "やぼったい",
                     new String[] {"やぼったい"});
  }

  public void testTsukitosha() throws Exception {
    assertAnalyzesTo(analyzer, "突き通しゃ",
                     new String[] {"突き通しゃ"});
    assertAnalyzesTo(analyzerWithCompounds, "突き通しゃ",
                     new String[] {"突き通しゃ"});
  }

  public void testBocchan() throws Exception {
    doTestBocchan(1, true);
    doTestBocchan(1, false);
  }

  @Nightly
  public void testBocchanBig() throws Exception {
    doTestBocchan(100, true);
    doTestBocchan(100, false);
  }
  
  private void doTestBocchan(int numIterations, boolean includeCompounds) throws Exception {
    LineNumberReader reader = new LineNumberReader(new InputStreamReader(
        this.getClass().getResourceAsStream("bocchan.utf-8")));
    
    String line = reader.readLine();
    reader.close();
    
    if (VERBOSE) {
      System.out.println("Test for Bocchan without pre-splitting sentences");
    }

    /*
    if (numIterations > 1) {
      // warmup
      for (int i = 0; i < numIterations; i++) {
        final TokenStream ts = analyzer.tokenStream("ignored", new StringReader(line));
        ts.reset();
        while(ts.incrementToken());
      }
    }
    */

    long totalStart = System.currentTimeMillis();
    for (int i = 0; i < numIterations; i++) {
      final Analyzer a = includeCompounds ? analyzerWithCompounds : analyzer;
      final TokenStream ts = a.tokenStream("ignored", new StringReader(line));
      ts.reset();
      while(ts.incrementToken());
    }
    String[] sentences = line.split("、|。");
    if (VERBOSE) {
      System.out.println("Total time : " + (System.currentTimeMillis() - totalStart));
      System.out.println("Test for Bocchan with pre-splitting sentences (" + sentences.length + " sentences)");
    }
    totalStart = System.currentTimeMillis();
    for (int i = 0; i < numIterations; i++) {
      for (String sentence: sentences) {
        final TokenStream ts = analyzer.tokenStream("ignored", new StringReader(sentence));
        ts.reset();
        while(ts.incrementToken());
      }
    }
    if (VERBOSE) {
      System.out.println("Total time : " + (System.currentTimeMillis() - totalStart));
    }
  }


  // nocommit need a still better test, where an input is
  // tokenized to multiple tokens that would be different
  // w/o the user dict...

  // nocommit need test where userdict has multiple matches
  // from one spot (ie, some entries are prefixes)...
}