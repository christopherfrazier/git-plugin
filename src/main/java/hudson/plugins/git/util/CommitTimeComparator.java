/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
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
package hudson.plugins.git.util;

import hudson.plugins.git.Revision;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.IOException;
import java.util.Comparator;

/**
 * Compares {@link Revision} by their timestamps.
 * 
 * @author Kohsuke Kawaguchi
 */
public class CommitTimeComparator implements Comparator<Revision> {
    private final RevWalk walk;

    public CommitTimeComparator(Repository r) {
        walk = new RevWalk(r);
    }

    public int compare(Revision lhs, Revision rhs) {
        return compare(time(lhs),time(rhs));
    }

    private int time(Revision r) {
        // parseCommit caches parsed Commit objects, so this is reasonably efficient.
        try {
            return walk.parseCommit(r.getSha1()).getCommitTime();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse "+r.getSha1(),e);
        }
    }

    private int compare(int lhs, int rhs) {
        if (lhs<rhs)    return -1;
        if (lhs>rhs)    return 1;
        return 0;
    }

    /**
     * Returns a boolean indicating whether the revision was
     * committed within the specified number of hours.
     * If we cannot parse the commit time, return False.
     */
    public boolean isWithinCutoff(Revision rev, int cutoffHours) {

        // Invariant: cutoffHours should be non-negative
        // (cutoff of zero indicates that every commit
        // should be excluded; useful for testing).
        assert cutoffHours >= 0;

        long currentTime = System.currentTimeMillis();
        long cutoffMilliseconds = cutoffHours * 3600000;

        try {
            // Convert the commit time (in seconds) to milliseconds
            long commitTime = (long)time(rev) * 1000;

            // Check whether the commit time is after the cutoff
            // Note that if the commit time is in the future
            // (which should never happen) this will always return true.
            return (currentTime - commitTime) < cutoffMilliseconds;

        } catch (RuntimeException e) {

            // If we can't parse the commit time, assume that it is
            // NOT within the cutoff.
            return false;
        }
    }
}
