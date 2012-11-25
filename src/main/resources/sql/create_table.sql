--
--
-- Copyright to the original author or authors.
--
-- Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
-- compliance with the License. You may obtain a copy of the License at:
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software distributed under the License is
-- distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and limitations under the License.
--

create table plugin (
    id long primary key auto_increment,
    group_id varchar(255),
    artifact_id varchar(255),
    version varchar(255)
);

create table project (
    id long primary key auto_increment,
    group_id varchar(255),
    artifact_id varchar(255)
);

create table plugin_execution (
    id long primary key auto_increment,
    project_id long,
    plugin_id long,
    goal varchar(255),
    execution_id varchar(255),
    execution_hashcode int,
    start_time timestamp,
    end_time timestamp,
    elapsed_millis_time long as ABS(DATEDIFF('MILLISECOND', end_time, start_time)),
    result varchar(255)
);

alter table plugin_execution
    add constraint fk_plugin_execution_to_project
        foreign key (project_id) references project(id);

alter table plugin_execution
    add constraint fk_plugin_execution_to_plugin
        foreign key (plugin_id) references plugin(id);
