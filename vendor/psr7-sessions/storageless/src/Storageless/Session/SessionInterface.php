<?php
/*
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many individuals
 * and is licensed under the MIT license.
 */

declare(strict_types=1);

namespace PSR7Sessions\Storageless\Session;

use JsonSerializable;

interface SessionInterface extends JsonSerializable
{
    /**
     * Stores a given value in the session
     *
     * @param int|bool|string|float|mixed[]|object|JsonSerializable|null $value allows any nested combination of the previous
     * types as well
     */
    public function set(string $key, $value) : void;

    /**
     * Retrieves a value from the session - if the value doesn't exist, then it uses the given $default, but transformed
     * into a immutable and safely manipulated scalar or array
     *
     * @param int|bool|string|float|mixed[]|object|JsonSerializable|null $default
     *
     * @return int|bool|string|float|mixed[]|null
     */
    public function get(string $key, $default = null);

    /**
     * Removes an item from the session
     */
    public function remove(string $key) : void;

    /**
     * Clears the contents of the session
     */
    public function clear() : void;

    /**
     * Checks whether a given key exists in the session
     */
    public function has(string $key) : bool;

    /**
     * Checks whether the session has changed its contents since its lifecycle start
     */
    public function hasChanged() : bool;

    /**
     * Checks whether the session contains any data
     */
    public function isEmpty() : bool;

    /** {@inheritDoc} */
    public function jsonSerialize() : object;
}
