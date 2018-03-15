module Auth.Authorization ( getLoggedUser, routheIfAuthorized, authenticateConfig, passwordConfig, tlsConf ) where

import Data.Acid              ( AcidState )
import Control.Monad.IO.Class ( liftIO )
import Data.Time              ( getCurrentTime )

import Web.Routes                           ( RouteT(..), nestURL, mapRouteT )
import Happstack.Server                     ( ServerPartT, Response, unauthorized, getHeaderM
                                            , mapServerPartT, toResponse )
import Happstack.Authenticate.Core          ( AuthenticateURL(..), AuthenticateState
                                            , decodeAndVerifyToken, usernamePolicy
                                            , AuthenticateConfig(..), Token(..) )
import Happstack.Authenticate.Password.Core ( PasswordConfig(..) )
import Happstack.Server.SimpleHTTPS         ( TLSConf(..), nullTLSConf  )
import Happstack.Foundation                 ( lift )
import qualified Happstack.Authenticate.Core as AuthUser

import Route.PageEnum              ( Sitemap(..) )
import Controller.AcidHelper       ( CtrlV, App )

import qualified Data.Domain.User      as DomainUser
import qualified Data.ByteString.Char8 as B
import qualified Data.Text.Encoding as T
import qualified Data.Text as T


routheIfAuthorized :: AcidState AuthenticateState -> (Sitemap -> DomainUser.User -> CtrlV)
                    -> Sitemap -> CtrlV
routheIfAuthorized authenticateState route url =
    do  mAuth <- getHeaderM "Authorization"
        case mAuth of
            Nothing -> unauthorized $ toResponse "You are not authorized."
            (Just auth') ->
                do  let auth = B.drop 7 auth'
                    now <- liftIO getCurrentTime
                    mToken <- decodeAndVerifyToken authenticateState now (T.decodeUtf8 auth)
                    case mToken of
                        Nothing -> unauthorized $ toResponse "You are not authorized."
                        (Just (token,_)) -> route url (getLoggedUser (_tokenUser token))

getLoggedUser :: AuthUser.User -> DomainUser.User
getLoggedUser authUser = undefined

authenticateConfig :: AuthenticateConfig
authenticateConfig = AuthenticateConfig
             { _isAuthAdmin        = const $ return True
             , _usernameAcceptable = usernamePolicy
             , _requireEmail       = True
             }

passwordConfig :: PasswordConfig
passwordConfig = PasswordConfig
             { _resetLink = T.pack "https://localhost:8443/#resetPassword"
             , _domain    = T.pack "example.org"
             , _passwordAcceptable = \t ->
                 if T.length t >= 5
                 then Nothing
                 else Just $ T.pack "Must be at least 5 characters."
             }

tlsConf :: TLSConf
tlsConf =
    nullTLSConf { tlsPort = 8443
                , tlsCert = "library/Auth/ssl/localhost.crt"
                , tlsKey  = "library/Auth/ssl/localhost.key"
                }

