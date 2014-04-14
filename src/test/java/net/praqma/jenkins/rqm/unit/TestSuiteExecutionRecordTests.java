/*
 * The MIT License
 *
 * Copyright 2014 Mads.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.praqma.jenkins.rqm.unit;

import net.praqma.jenkins.rqm.model.TestSuiteExecutionRecord;
import org.apache.commons.httpclient.NameValuePair;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author Mads
 */
public class TestSuiteExecutionRecordTests {
    
    @Test
    public void correctQueryString() {
        String suites = "mysuite1,mysuite2";
        NameValuePair[] actual = TestSuiteExecutionRecord.getFilteringProperties("myshost", 9090, "qm", suites);
        NameValuePair[] expected = { new NameValuePair("fields", "feed/entry/content/suiteexecutionrecord[title='mysuite1' or title='mysuite2']/*") };
        assertArrayEquals(expected, actual);
        
        String suiteEmpty = "mysuite1,";        
        NameValuePair[] actual2 = TestSuiteExecutionRecord.getFilteringProperties("myshost", 9090, "qm", suiteEmpty);
        NameValuePair[] expected2 = { new NameValuePair("fields", "feed/entry/content/suiteexecutionrecord[title='mysuite1']/*") };
        assertArrayEquals(expected2, actual2);  
        
        String suiteSingle = "mysuite1";        
        NameValuePair[] actual3 = TestSuiteExecutionRecord.getFilteringProperties("myshost", 9090, "qm", suiteSingle);
        NameValuePair[] expected3 = { new NameValuePair("fields", "feed/entry/content/suiteexecutionrecord[title='mysuite1']/*") };
        assertArrayEquals(expected3, actual3);          
    }
}
