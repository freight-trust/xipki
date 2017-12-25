/*
 *
 * Copyright (c) 2013 - 2018 Lijun Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.xipki.security.speed.cmd.completer;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.xipki.console.karaf.AbstractEnumCompleter;

/**
 * @author Lijun Liao
 * @since 2.2.0
 */

@Service
//CHECKSTYLE:SKIP
public class GMACSigAlgCompleter extends AbstractEnumCompleter {

    public GMACSigAlgCompleter() {
        setTokens("AES128-GMAC,AES192-GMAC,AES256-GMAC");
    }

}
