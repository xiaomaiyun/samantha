/*
 * Copyright (c) [2016-2018] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class SequenceTstampSplitExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> beforeAttrs;
    final private List<String> afterAttrs;
    final private String tstampAttr;
    final private int splitTstamp;
    final private String separator;
    final private String joiner;

    public SequenceTstampSplitExpander(List<String> nameAttrs, String separator, String joiner,
                                       String tstampAttr, int splitTstamp, List<String> beforeAttrs,
                                       List<String> afterAttrs) {
        this.nameAttrs = nameAttrs;
        this.joiner = joiner;
        this.separator = separator;
        this.splitTstamp = splitTstamp;
        this.tstampAttr = tstampAttr;
        this.beforeAttrs = beforeAttrs;
        this.afterAttrs = afterAttrs;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Integer splitTstamp = expanderConfig.getInt("splitTstamp");
        if (splitTstamp == null) {
            splitTstamp = 0;
        }
        return new SequenceTstampSplitExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getString("tstampAttr"), splitTstamp,
                expanderConfig.getStringList("beforeAttrs"),
                expanderConfig.getStringList("afterAttrs"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            List<String[]> values = new ArrayList<>();
            for (String nameAttr : nameAttrs) {
                String[] splitted = entity.get(nameAttr).asText().split(separator, -1);
                values.add(splitted);
            }
            String[] fstamp = entity.get(tstampAttr).asText().split(separator, -1);
            int size = fstamp.length;
            int split;
            for (split=0; split<size; split++) {
                String tstamp = fstamp[split];
                int istamp = Integer.parseInt(tstamp);
                if (istamp >= splitTstamp) {
                    break;
                }
            }
            for (int i=0; i<nameAttrs.size(); i++) {
                entity.put(beforeAttrs.get(i), StringUtils.join(
                        ArrayUtils.subarray(values.get(i), 0, split), joiner));
                entity.put(afterAttrs.get(i), StringUtils.join(
                        ArrayUtils.subarray(values.get(i), split, size), joiner));
            }
        }
        return initialResult;
    }
}
