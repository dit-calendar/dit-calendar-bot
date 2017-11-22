{-# LANGUAGE FlexibleContexts, GeneralizedNewtypeDeriving,
    MultiParamTypeClasses, OverloadedStrings, ScopedTypeVariables,
    TypeFamilies, FlexibleInstances #-}

module Controller.AcidHelper ( CtrlV, withAcid, Acid, App(..) ) where

import Prelude
import System.FilePath      ( (</>) )
import Data.Maybe           ( fromMaybe )
import Control.Exception    ( bracket )
import Control.Monad        ( MonadPlus )
import Control.Monad.Reader ( MonadReader, ReaderT, ask )
import Control.Monad.Trans  ( MonadIO(..) )
import Control.Applicative  ( Applicative, Alternative )

import Data.Acid            ( openLocalStateFrom, AcidState(..) )
import Data.Acid.Local      ( createCheckpointAndClose )
import Happstack.Server
    ( Happstack, HasRqData, Response, ServerPartT(..)
    , WebMonad, FilterMonad, ServerMonad )
import Happstack.Foundation ( HasAcidState(..) )

import qualified Data.Repository.Acid.UserAcid          as UserAcid
import qualified Data.Repository.Acid.CalendarAcid      as CalendarAcid
import qualified Data.Repository.Acid.TaskAcid          as TaskAcid


newtype App a = App { unApp :: ServerPartT (ReaderT Acid IO) a }
    deriving ( Functor, Alternative, Applicative, Monad
             , MonadPlus, MonadIO, HasRqData, ServerMonad
             , WebMonad Response, FilterMonad Response
             , Happstack, MonadReader Acid
             )
type CtrlV   = App Response

data Acid = Acid
   {
     acidUserListState         :: AcidState UserAcid.UserList
     , acidEntryListState      :: AcidState CalendarAcid.EntryList
     , acidTaskListState       :: AcidState TaskAcid.TaskList
   }

instance HasAcidState App UserAcid.UserList where
    getAcidState = acidUserListState <$> ask

instance HasAcidState App CalendarAcid.EntryList where
    getAcidState = acidEntryListState <$> ask

instance HasAcidState App TaskAcid.TaskList where
    getAcidState = acidTaskListState <$> ask

withAcid :: Maybe FilePath -- ^ state directory
         -> (Acid -> IO a) -- ^ action
         -> IO a
withAcid mBasePath f =
    let basePath = fromMaybe "state" mBasePath
        userPath = basePath </> "userList"
        calendarEntryPath = basePath </> "entryList"
        taskPath = basePath </> "taskList"
    in bracket (openLocalStateFrom userPath UserAcid.initialUserListState) createCheckpointAndClose $ \c ->
       bracket (openLocalStateFrom calendarEntryPath CalendarAcid.initialEntryListState) createCheckpointAndClose $ \g ->
       bracket (openLocalStateFrom taskPath TaskAcid.initialTaskListState) createCheckpointAndClose $ \t ->
        f (Acid c g t)
