{-# LANGUAGE FlexibleContexts #-}

module Data.Repository.CalendarTaskRepo where

import Happstack.Foundation     ( query, update, HasAcidState )
import Control.Monad.IO.Class
import Data.List                ( delete )
import Data.Maybe               ( fromJust )

import Data.Domain.CalendarEntry            as CalendarEntry
import Data.Domain.Types        ( UserId, EntryId )
import Data.Domain.Task                     as Task

import qualified Data.Repository.TaskRepo             as TaskRepo
import qualified Data.Repository.CalendarRepo         as CalendarRepo
import qualified Data.Repository.Acid.UserAcid        as UserAcid
import qualified Data.Repository.Acid.TaskAcid        as TaskAcid
import qualified Data.Repository.Acid.CalendarAcid    as CalendarAcid



deleteCalendarsTasks :: (HasAcidState m UserAcid.UserList, HasAcidState m TaskAcid.TaskList, MonadIO m)
    => CalendarEntry -> m ()
deleteCalendarsTasks calendar =
    foldr (\ x ->
      (>>) (do
        mTask <- query (TaskAcid.TaskById x)
        TaskRepo.deleteTask (fromJust mTask) ))
    (return ()) $ CalendarEntry.calendarTasks calendar

createTask :: (HasAcidState m CalendarAcid.EntryList,
      HasAcidState m TaskAcid.TaskList, MonadIO m) =>
    CalendarEntry -> String -> m Task
createTask calendarEntry description =
    do
        mTask <- update $ TaskAcid.NewTask description
        CalendarRepo.addTaskToCalendarEntry (Task.taskId mTask) calendarEntry
        return mTask