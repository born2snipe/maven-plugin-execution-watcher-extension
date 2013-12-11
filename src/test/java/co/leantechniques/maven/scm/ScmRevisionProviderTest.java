/**
 *
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package co.leantechniques.maven.scm;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openide.util.Lookup;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScmRevisionProviderTest {
    @Mock
    private Lookup lookup;
    @Mock
    private ScmRevisionQuery query;
    @Mock
    private ScmRevisionQuery otherQuery;
    private ScmRevisionProvider revisionProvider;
    private File directory;
    private CodeRevision codeRevision;

    @Before
    public void setUp() throws Exception {
        directory = new File("directory");
        codeRevision = new CodeRevision("", "");

        revisionProvider = new ScmRevisionProvider(lookup);
    }

    @Test
    public void shouldReturnAnEmptyCodeRevisionWhenNoQuerySupportsTheCurrentScm() {
        when(lookup.lookupAll(ScmRevisionQuery.class)).thenReturn(new ArrayList(Arrays.asList(query)));

        assertEmptyRevision(revisionProvider.determineRevisionOf(directory));
    }

    @Test
    public void shouldUseTheScmRevisionFromTheQueryThatSupportsTheScmTool() {
        when(lookup.lookupAll(ScmRevisionQuery.class)).thenReturn(new ArrayList(Arrays.asList(query, otherQuery)));
        when(otherQuery.supports(directory)).thenReturn(true);
        when(otherQuery.queryRevision(directory)).thenReturn(codeRevision);

        assertSame(codeRevision, revisionProvider.determineRevisionOf(directory));
    }

    @Test
    public void shouldAlwaysReturnACodeRevision() {
        when(lookup.lookupAll(ScmRevisionQuery.class)).thenReturn(new ArrayList());

        assertEmptyRevision(revisionProvider.determineRevisionOf(directory));
    }

    private void assertEmptyRevision(CodeRevision codeRevision) {
        assertNull(codeRevision.revision);
        assertNull(codeRevision.scm);
    }
}
