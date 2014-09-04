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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.praqma.jenkins.rqm.model.TestCase;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Mads
 */
public class TestCaseTests {
    
    @Test
    public void sortByExecutionOrder()  {
        TestCase tc1 = new TestCase();
        tc1.setExecutionOrder(1);
        tc1.setTestCaseTitle("first");
        
        TestCase tc2 = new TestCase();
        tc2.setExecutionOrder(5);
        tc2.setTestCaseTitle("third");
        
        TestCase tc3 = new TestCase();
        tc3.setExecutionOrder(2);
        tc3.setTestCaseTitle("second");
        
        List<TestCase> cases = Arrays.asList(tc1,tc2,tc3);
        Collections.sort(cases);
        
        assertEquals("first", cases.get(0).getTestCaseTitle());
        assertEquals("second", cases.get(1).getTestCaseTitle());
        assertEquals("third", cases.get(2).getTestCaseTitle());
                
        
    }
}
