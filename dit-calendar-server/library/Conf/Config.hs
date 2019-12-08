{-# LANGUAGE OverloadedStrings #-}

module Conf.Config
    ( readConfig
    , Config(..)
    , LocalConfig(..)
    ) where

import           Data.Ini.Config
import           Data.Text

import           Conf.NetworkConfig (NetworkConfig (..), networkConfigParser)

data Config = Config
    { cfNetwork :: NetworkConfig
    , cfLocal   :: Maybe LocalConfig
    } deriving (Eq, Show)

data LocalConfig = LocalConfig
    { adminUser     :: Text
    , adminPassword :: Text
    } deriving (Eq, Show)

localConfigParser :: IniParser (Maybe LocalConfig)
localConfigParser = 
    sectionMb "LOCAL" $ do
        user <- fieldOf "admin.user" string
        password <- fieldOf "admin.password" string
        return LocalConfig {adminPassword = password, adminUser = user}

configParser :: IniParser Config
configParser = do
    netCf <- networkConfigParser
    locCf <- localConfigParser
    return Config {cfNetwork = netCf, cfLocal = locCf}

readConfig :: Text -> Either String Config
readConfig textConfig = parseIniFile textConfig configParser
