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
package co.leantechniques.maven;

import co.leantechniques.maven.h2.H2BuildInformationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openide.util.Lookup;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuildInformationRepositoryProviderTest {
    @Mock
    private BuildInformationRepository repository;
    @Mock
    private Lookup lookup;
    private BuildInformationRepositoryProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new BuildInformationRepositoryProvider(lookup);
    }

    @Test
    public void shouldUseTheCustomRepositoryIfOneIsProvided() {
        when(lookup.lookup(BuildInformationRepository.class)).thenReturn(repository);
        assertSame(repository, provider.provide());
    }

    @Test
    public void shouldProvideTheH2Repository() {
        when(lookup.lookup(BuildInformationRepository.class)).thenReturn(null);
        assertTrue(provider.provide() instanceof H2BuildInformationRepository);
    }

}
