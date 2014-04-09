/*
 * The MIT License
 *
 * Copyright 2013 Praqma.
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
package net.praqma.jenkins.rqm;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.util.List;
import net.praqma.jenkins.rqm.model.RqmObject;
import net.praqma.jenkins.rqm.request.RqmParameterList;

/**
 *
 * @author Praqma
 * @param <T> The type of object you wish to be created.
 */
public class RqmObjectCreator<T extends RqmObject> implements FilePath.FileCallable<List<T>> {
    private final RqmParameterList parameters;
    private final T target;
 
    public RqmObjectCreator(Class<T> target, RqmParameterList parameters) throws IllegalAccessException, InstantiationException {
        this.parameters = parameters;
        this.target = target.newInstance();
    }

    @Override
    public List<T> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        if(parameters.methodType.equals("GET")) {
            return (List<T>)target.readMultiple(parameters);
        } else {
            return (List<T>)target.createOrUpdate(parameters);
        }        
    }
}
