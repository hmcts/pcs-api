const { retry } = require('./retry.helper.js');

export const request = (
  url: string,
  headers: Record<string, string>,
  body: BodyInit | undefined,
  method = 'POST'
): Promise<Response> =>
  fetch(url, {
    method,
    body,
    headers,
  });
export const retriedRequest = async (
  url: string,
  headers: Record<string, string>,
  body: BodyInit,
  method: string = 'POST',
  expectedStatus = 201
): Promise<Response> => {
  return retry(() => {
    return request(url, headers, body, method).then(response => {
      if (response.status !== expectedStatus) {
        throw new Error(
          `Expected status: ${expectedStatus}, actual status: ${response.status}, ` +
            `message: ${response.statusText}, url: ${response.url}`
        );
      }
      return response;
    });
  });
};

module.exports = { request, retriedRequest };
