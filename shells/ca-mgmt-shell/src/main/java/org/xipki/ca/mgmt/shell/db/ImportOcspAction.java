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

package org.xipki.ca.mgmt.shell.db;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.xipki.ca.mgmt.db.port.DbPortWorker;
import org.xipki.ca.mgmt.db.port.ocsp.OcspDbImportWorker;
import org.xipki.shell.completer.DirCompleter;

/**
 * TODO.
 * @author Lijun Liao
 * @since 2.0.0
 */

@Command(scope = "ca", name = "import-ocsp", description = "import OCSP database")
@Service
public class ImportOcspAction extends DbPortAction {

  @Option(name = "--db-conf", required = true, description = "database configuration file")
  @Completion(FileCompleter.class)
  private String dbconfFile;

  @Option(name = "--in-dir", required = true, description = "input directory")
  @Completion(DirCompleter.class)
  private String indir;

  @Option(name = "-k", description = "number of certificates per commit")
  private Integer numCertsPerCommit = 100;

  @Option(name = "--resume", description = "resume from the last successful point")
  private Boolean resume = Boolean.FALSE;

  @Override
  protected DbPortWorker getDbPortWorker() throws Exception {
    return new OcspDbImportWorker(datasourceFactory, passwordResolver, dbconfFile, resume,
        indir, numCertsPerCommit.intValue());
  }

}