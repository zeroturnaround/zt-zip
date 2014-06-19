package org.zeroturnaround.zip;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author innokenty
 */
class ListNameMapper implements NameMapper {

    private final Queue queue;

    public ListNameMapper(String[] names) {
        queue = new LinkedList(Arrays.asList(names));
    }

    public String map(String name) {
        return (String) queue.poll();
    }

}
