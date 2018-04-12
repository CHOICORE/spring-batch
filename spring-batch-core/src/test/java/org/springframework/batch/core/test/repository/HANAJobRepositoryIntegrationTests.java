/*
 * Copyright 2020-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.test.repository;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.utility.DockerImageName;

import com.sap.db.jdbcext.HanaDataSource;

/**
 * @author Jonathan Bregler
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class HANAJobRepositoryIntegrationTests {

	private static final DockerImageName HANA_IMAGE = DockerImageName.parse( "store/saplabs/hanaexpress:2.00.054.00.20210603.1" );

	@ClassRule
	public static HANAContainer<?> hana = new HANAContainer<>( HANA_IMAGE ).acceptLicense();

	@Autowired
	private DataSource dataSource;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private Job job;

	@Before
	public void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript( new ClassPathResource( "/org/springframework/batch/core/schema-hana.sql" ) );
		databasePopulator.execute( this.dataSource );
	}

	@Test
	public void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobLauncher.run( this.job, jobParameters );

		// then
		Assert.assertNotNull( jobExecution );
		Assert.assertEquals( ExitStatus.COMPLETED, jobExecution.getExitStatus() );
	}

	@Configuration
	@EnableBatchProcessing
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			HanaDataSource dataSource = new HanaDataSource();
			dataSource.setUser( hana.getUsername() );
			dataSource.setPassword( hana.getPassword() );
			dataSource.setUrl( hana.getJdbcUrl() );
			return dataSource;
		}

		@Bean
		public Job job(JobBuilderFactory jobs, StepBuilderFactory steps) {
			return jobs.get( "job" )
					.start( steps.get( "step" ).tasklet( (contribution, chunkContext) -> RepeatStatus.FINISHED ).build() )
					.build();
		}

	}
}
