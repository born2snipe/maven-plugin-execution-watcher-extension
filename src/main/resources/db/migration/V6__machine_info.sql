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

create table if not exists machine_info (
    id long primary key auto_increment,
    username varchar(255),
    maven_version varchar(255),
    java_version varchar(255),
    computer_name varchar(255),
    os varchar(255),
    os_arch varchar(255)
);

alter table build add column machine_info_id long;

alter table build
    add constraint fk_machine_info_to_build
        foreign key (machine_info_id) references machine_info(id);

