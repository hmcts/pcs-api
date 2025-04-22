curl -L -X POST 'https://idam-web-public.aat.platform.hmcts.net/o/token' \
-H 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'username=username' \
--data-urlencode 'password=password' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=id' \
--data-urlencode 'client_secret=client_secret' \
--data-urlencode 'redirect_uri=https://civil-citizen-ui.aat.platform.hmcts.net/oauth2/callback' \
--data-urlencode 'scope=profile roles'
