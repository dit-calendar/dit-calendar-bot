{-# LANGUAGE FlexibleContexts      #-}
{-# LANGUAGE FlexibleInstances     #-}
{-# LANGUAGE KindSignatures        #-}
{-# LANGUAGE MultiParamTypeClasses #-}
{-# LANGUAGE OverloadedStrings     #-}
{-# LANGUAGE QuasiQuotes           #-}
{-# LANGUAGE TemplateHaskell       #-}

module Data.Service.UserTasksSpec (spec) where

import           Control.Monad.TestFixture
import           Control.Monad.TestFixture.TH
import           Data.Default                 (def)
import           Test.Hspec

import           Control.Monad.Identity       (Identity)
import           Control.Monad.IO.Class
import           Control.Monad.Writer.Class   (tell)

import           Data.Domain.Task             as Task
import           Data.Domain.User             as User
import           Data.Repository.CalendarRepo (MonadDBCalendarRepo)
import           Data.Repository.TaskRepo     (MonadDBTaskRepo)
import           Data.Repository.UserRepo     (MonadDBUserRepo)

import qualified Data.Service.UserTasks       as UserTasksService


mkFixture "Fixture" [ts| MonadDBUserRepo, MonadDBTaskRepo |]

userFromDb = def{ loginName="Foo", User.userId=10, assignedToTasks=[1,2,3] }
userFromDb2 = def{ loginName="Foo1", User.userId=8, assignedToTasks=[1,2] }
taskFromDb = def{ Task.description="task1", taskId=5, startTime=Nothing, endTime=Nothing, assignedUsers=[8,11]}
taskFromDb2 = def{ Task.description="task2", taskId=6, startTime=Nothing, endTime=Nothing, assignedUsers=[8]}

fixture :: (Monad m, MonadWriter [String] m) => Fixture m
fixture = Fixture {  _updateTask = \a -> tell [show a] >>= (\_ -> return $ Right a)
                  , _findTaskById = \a -> return $ Just (if a == 1 then taskFromDb else taskFromDb2)
                  , _addTaskToUser = \user taskId -> tell [show user] >> tell [show taskId] >>= (\_ -> return $ Right user)
                  , _deleteTaskFromUser = \x a -> tell [show x] >> tell [show a] >>= (\_ -> return $ Right x)
                  , _findUserById = \a -> return $ Just (if a == 7 then userFromDb else userFromDb2)
                  }

instance MonadIO Identity where
    liftIO = undefined


spec = describe "UserTasksSpec" $ do
    it "deleteTaskFromAllUsers" $ do
        let task = def{ Task.description="task1", taskId=1, assignedUsers=[7,8], startTime=Nothing, endTime=Nothing}
        let (_, log) = evalTestFixture (UserTasksService.deleteTaskFromAllUsersImpl task) fixture
        length log `shouldBe` 4
        -- deleteTaskFromUser calls
        log!!0 `shouldBe` show userFromDb
        log!!1 `shouldBe` show task
        log!!2 `shouldBe` show userFromDb2
        log!!3 `shouldBe` show task
    it "addUserToTask" $ do
        let task = def { Task.description="task1", taskId=1, assignedUsers=[2], startTime=Nothing, endTime=Nothing}
        let expectedTask = def { Task.description="task1", taskId=1, assignedUsers=[10, 2], startTime=Nothing, endTime=Nothing}
        let (_, log) = evalTestFixture (UserTasksService.addUserToTaskImpl task userFromDb) fixture
        length log `shouldBe` 3
        log!!0 `shouldBe` show userFromDb
        log!!1 `shouldBe` show (Task.taskId task)
        log!!2 `shouldBe` show expectedTask
    it "removeUserFromTask" $ do
        let task = def { Task.description="task1", taskId=1, assignedUsers=[2,10], startTime=Nothing, endTime=Nothing}
        let expectedTask = def { Task.description="task1", taskId=1, assignedUsers=[2], startTime=Nothing, endTime=Nothing}
        let (_, log) = evalTestFixture (UserTasksService.removeUserFromTaskImpl task userFromDb) fixture
        length log `shouldBe` 3
        log!!0 `shouldBe` show userFromDb
        log!!1 `shouldBe` show task
        log!!2 `shouldBe` show expectedTask
    it "removeUserFromTasks" $ do
        let (_, log) = evalTestFixture (UserTasksService.removeUserFromTasksImpl userFromDb2) fixture
        length log `shouldBe` 6
        log!!0 `shouldBe` show userFromDb2
        log!!1 `shouldBe` show taskFromDb
        log!!2 `shouldBe` show taskFromDb {assignedUsers=[11]}
        log!!3 `shouldBe` show userFromDb2
        log!!4 `shouldBe` show taskFromDb2
        log!!5 `shouldBe` show taskFromDb2 {assignedUsers=[]}
