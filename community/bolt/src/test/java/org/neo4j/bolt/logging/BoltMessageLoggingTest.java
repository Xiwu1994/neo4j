/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.logging;

import java.io.IOException;

import io.netty.channel.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.Log;
import org.neo4j.scheduler.JobScheduler;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.bolt_logging_enabled;
import static org.neo4j.helpers.collection.MapUtil.stringMap;

@RunWith( MockitoJUnitRunner.class )
public class BoltMessageLoggingTest
{
    @Mock( answer = Answers.RETURNS_MOCKS )
    private FileSystemAbstraction fs;
    @Mock( answer = Answers.RETURNS_MOCKS )
    private JobScheduler jobScheduler;
    @Mock( answer = Answers.RETURNS_MOCKS )
    private Log log;
    @Mock( answer = Answers.RETURNS_MOCKS )
    private Channel channel;

    @Test
    public void shouldCreateNullLoggerWhenDisabled()
    {
        Config config = newConfig( false );

        BoltMessageLogging logging = BoltMessageLogging.create( fs, jobScheduler, config, log );
        BoltMessageLogger logger = logging.newLogger( channel );

        assertThat( logger, instanceOf( NullBoltMessageLogger.class ) );
    }

    @Test
    public void shouldCreateNullLoggerWhenUnableToCreateRealLogger() throws IOException
    {
        Config config = newConfig( true );
        IOException fsError = new IOException();
        when( fs.openAsOutputStream( any(), anyBoolean() ) ).thenThrow( fsError );

        BoltMessageLogging logging = BoltMessageLogging.create( fs, jobScheduler, config, log );
        BoltMessageLogger logger = logging.newLogger( channel );

        assertThat( logger, instanceOf( NullBoltMessageLogger.class ) );
        verify( log ).warn( startsWith( "Unable to create bolt message log" ), eq( fsError ) );
    }

    @Test
    public void shouldCreateRealLoggerWhenEnabled()
    {
        Config config = newConfig( true );

        BoltMessageLogging logging = BoltMessageLogging.create( fs, jobScheduler, config, log );
        BoltMessageLogger logger = logging.newLogger( channel );

        assertThat( logger, instanceOf( BoltMessageLoggerImpl.class ) );
    }

    @Test
    public void shouldCreateNullLoggerWhenNone()
    {
        BoltMessageLogging logging = BoltMessageLogging.none();
        BoltMessageLogger logger = logging.newLogger( channel );

        assertThat( logger, instanceOf( NullBoltMessageLogger.class ) );
    }

    private static Config newConfig( boolean boltLogEnabled )
    {
        return Config.defaults( stringMap( bolt_logging_enabled.name(), Boolean.toString( boltLogEnabled ) ) );
    }
}
